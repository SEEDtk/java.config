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
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.theseed.basic.ParseFailureException;

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
			Iterator<File> iter = CodeBase.this.getTopProjects().iterator();
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
			List<File> subFiles = getSubmodules(retVal);
			// If the file list is nonempty, push it on the stack.
			if (subFiles != null && ! subFiles.isEmpty())
				this.dirStack.push(subFiles.iterator());
			return retVal;
		}

	}

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
	public List<File> getTopProjects() {
		return Arrays.asList(this.masterDir.listFiles(PROJ_DIR));
	}

	/**
	 * Find submodules in the specified project directory.
	 *
	 * @param projDir	project directory to check
	 *
	 * @return a list of directories for the submodules, or NULL if the project is a leaf project
	 */
	public List<File> getSubmodules(File projDir) {
		List<File> retVal = null;
		File moduleFile = new File(projDir, ".gitmodules");
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
			retVal = new ArrayList<File>(sections.size());
			// Loop through the sections, extracting the submodule name.
			for (String section : sections) {
				File subFile = new File(projDir, section);
				// Verify the directory exists.
				if (subFile.isDirectory())
					retVal.add(subFile);
			}
		}
		return retVal;
	}

	/**
	 * @return an iterator through all the project directories
	 */
	public Iterator<File> iterator() {
		return this.new ProjectIterator();
	}

	/**
	 * This method returns a git repo. It should be used in a try block. If the
	 * project does not exist, a parse failure exception is thrown.
	 *
	 * @param projName	name of a project in the current code base
	 *
	 * @return a repo for the specified project in this code base
	 *
	 * @throws IOException
	 * @throws ParseFailureException
	 */
	public GitRepo getRepo(String projName) throws IOException, ParseFailureException {
		GitRepo retVal;
		// Check to see if this is a high-level project. We do this first to see if we
		// can get a quick hit without a linear search.
		File projFile = new File(this.masterDir, projName);
		File gitDir = new File(projFile, ".git");
		if (gitDir.exists()) {
			// Here we can return the project immediately.
			retVal = new GitRepo(projFile);
		} else {
			// Now we have to search submodules, which is a long process. We iterate through all
			// the projects until we find the one we want.
			Iterator<File> iter = this.iterator();
			retVal = null;
			while (retVal == null && iter.hasNext()) {
				projFile = iter.next();
				if (projFile.getName().equals(projName))
					retVal = new GitRepo(projFile);
			}
			if (retVal == null)
				throw new ParseFailureException("Could not find " + projName
						+ " in code base " + this.masterDir);
		}
		return retVal;
	}

	@Override
	public String toString() {
		return "CodeBase [" + this.masterDir + "]";
	}





}
