/**
 *
 */
package org.theseed.config.git;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;

/**
 * This object manages a code base, which is a directory containing git modules, some of
 * which may have submodules.
 *
 * @author Bruce Parrello
 *
 */
public class CodeBase {

	// FIELDS
	/** master directory for the code base */
	private File masterDir;
	/** regex pattern for extracting submodule names */
	private Pattern SUBMODULE_NAME = Pattern.compile("\\[submodule \"([^\"])\\]");
	/** list filter for GIT subdirectories */
	private static FileFilter PROJ_DIR = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			boolean retVal = pathname.isDirectory();
			if (retVal) {
				File gitFile = new File(pathname, ".git");
				retVal = gitFile.exists();
			}
			return retVal;
		}
	};

	/**
	 * Construct a new code base directory manager.
	 *
	 * @param dir	master directory name
	 *
	 * @throws IOException
	 */
	public CodeBase(File dir) throws IOException {
		this.masterDir = dir;
		if (! this.masterDir.isDirectory())
			throw new IOException(dir + " is not a valid directory.");
	}

	/**
	 * @return a list of the top-level projects in this code base
	 */
	public List<File> getProjects() {
		return Arrays.asList(this.masterDir.listFiles(PROJ_DIR));
	}

	/**
	 * This class iterates through the subdirectories with projects in them and all
	 * the submodules of those projects. Modifications to the code base during
	 * iteration can cause unpredictable results, as we pre-generate each level
	 * of iteration.
	 *
	 * We will return a module before its sub-modules in a more-or-less depth-first
	 * fashion. The object maintains a stack of directory iterators. When we are
	 * about to return a module with sub-modules, we push the submodule iterator
	 * onto the stack. When the top iterator completes, we pop the stack.
	 */
	public class ProjectIterator implements Iterator<File> {

		/** stack of subdirectory iterators */
		private Deque<Iterator<File>> dirStack;

		/**
		 * Construct the iterator for this code base.
		 */
		public ProjectIterator() {
			// Initialize the stack.
			this.dirStack = new ArrayDeque<Iterator<File>>(3);
			// Get the list of main projects.
			Iterator<File> iter = CodeBase.this.getProjects().iterator();
			// If there are any projects, we push the iterator onto the stack.
			if (iter.hasNext())
				this.dirStack.push(iter);
		}

		@Override
		public boolean hasNext() {
			return ! this.dirStack.isEmpty();
		}

		@Override
		public File next() {
			if (this.dirStack.isEmpty())
				throw new NoSuchElementException("Iterated past end of code base " + CodeBase.this.masterDir
						+ ".");
			Iterator<File> topIter = this.dirStack.peek();
			File retVal = topIter.next();
			// We have two special cases now. If the current directory is the last, we pop
			// the stack. Then if the current directory has submodules, we push
			// a new iterator on the stack. Note that a completed iterator is never allowed
			// to be on the stack.
			if (! topIter.hasNext())
				this.dirStack.pop();
			// Figuring out the submodules is harder. If there is a ".gitmodules" file, we parse
			// it for the submodule list. This is to avoid the high overhead of creating and
			// discarding a "GitRepo" object.
			File moduleFile = new File(retVal, ".gitmodules");
			if (moduleFile.exists()) {
				FileBasedConfig subConfig = new FileBasedConfig(moduleFile, FS.DETECTED);
				try {
					subConfig.load();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				} catch (ConfigInvalidException e) {
					throw new RuntimeException(e.toString());
				}
				// The submodules are all names in the "submodule" section.
				Set<String> sections = subConfig.getSubsections("submodule");
				List<File> subFiles = new ArrayList<File>(sections.size());
				// Loop through the sections, extracting the submodule name.
				for (String section : sections) {
					File subFile = new File(retVal, section);
					// Verify the directory exists.
					if (subFile.isDirectory())
						subFiles.add(subFile);
				}
				// If the file list is nonempty, push it on the stack.
				if (! subFiles.isEmpty())
					this.dirStack.push(subFiles.iterator());
			}
			return retVal;
		}

	}

	// TODO data members for CodeBase

	// TODO constructors and methods for CodeBase
}
