/**
 *
 */
package org.theseed.config.git;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.BaseProcessor;
import org.theseed.basic.ParseFailureException;

/**
 * This is a command-processor base class for GIT-processing commands. It
 * creates a CodeBase object for accessing the projects in the specified
 * directory.
 *
 * The command-line options are as follows:
 *
 * -h	display command-line usage
 * -v	display more frequent log messages
 *
 * --base		module directory base (default is value of CODE_BASE (if any), else the current directory)
 *
 * @author Bruce Parrello
 *
 */
public abstract class BaseGitProcessor extends BaseProcessor {

	// FIELDS
	/** logging facility */
	protected static Logger log = LoggerFactory.getLogger(BaseGitProcessor.class);
	/** master code base */
	private CodeBase codeBase;

	// COMMAND-LINE OPTIONS

	/** code base directory name override */
	@Option(name = "--base", metaVar = "projDir", usage = "name of the master project directory")
	private File baseDir;

	@Override
	final protected void setDefaults() {
		String baseString = System.getenv("CODE_BASE");
		if (StringUtils.isBlank(baseString)) {
			baseString = System.getProperty("user.dir");
		}
		this.baseDir = new File(baseString);
		// Allow the subclass to set more defaults.
		this.setGitDefaults();
	}

	/**
	 * Set default parameters for a GIT command processor.
	 */
	protected abstract void setGitDefaults();

	@Override
	final protected boolean validateParms() throws IOException, ParseFailureException {
		// Validate the base directory.
		if (! this.baseDir.isDirectory())
			throw new FileNotFoundException("Code base directory " + this.baseDir + " is not found or invalid.");
		this.codeBase = new CodeBase(this.baseDir);
		// Allow the subclass to validate its parameters.
		this.validateGitParms();
		return true;
	}

	/**
	 * Validate the options and parameters for this command. Do not open a GitRepo
	 * in here or you risk a resource leak.
	 */
	protected abstract void validateGitParms();

	@Override
	final protected void runCommand() throws Exception {
		this.runGitCommand();
	}

	/**
	 * Process this GIT-related command function.
	 */
	protected abstract void runGitCommand();

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
		GitRepo retVal = this.codeBase.getRepo(projName);
		return retVal;
	}

	/**
	 * @return the code base directory
	 */
	public File getBaseDir() {
		return this.baseDir;
	}


}
