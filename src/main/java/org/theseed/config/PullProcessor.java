/**
 *
 */
package org.theseed.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.BaseProcessor;
import org.theseed.basic.ParseFailureException;
import org.theseed.config.git.GitRepo;

/**
 * This subcommand pulls a single GIT repo to update it to the latest version. It is used
 * to do spot fixes on existing code.
 *
 * The positional parameter is the name of the project to update.
 *
 * The command-line options are as follows:
 *
 * -h	display command-line usage
 * -v	display more frequent log messages
 * -b	branch to pull (default "master")
 * -r	remote to pull (default "origin")
 *
 * --base		module directory base (default is value of CODE_BASE (if any), else the current directory)
 * --shallow	if specified, submodules will not be pulled
 *
 * @author Bruce Parrello
 *
 */
public class PullProcessor extends BaseProcessor {

	// FIELDS
	/** logging facility */
	protected static Logger log = LoggerFactory.getLogger(PullProcessor.class);
	/** project directory */
	private File projDir;

	// COMMAND-LINE OPTIONS

	/** name of branch to pull */
	@Option(name = "--branch", aliases = { "-b" }, metaVar = "main",
			usage = "name of branch to pull")
	private String branchName;

	/** name of remote to use */
	@Option(name = "--remote", aliases = { "-r" }, metaVar = "develop",
			usage = "name of remote origin to use")
	private String remoteName;

	/** base directory for code projects */
	@Option(name = "--base", metaVar = "baseCodeDir", usage = "name of base directory for projects")
	private File baseDir;

	/** if TRUE, submodules will not be pulled */
	@Option(name = "--shallow", usage = "if specified, submodules will not be pulled")
	private boolean shallowFlag;

	/** name of the project to pull */
	@Argument(index = 0, metaVar = "projName", usage = "name of the code project to pull", required = true)
	private String projName;

	@Override
	protected void setDefaults() {
		this.branchName = "master";
		this.remoteName = "origin";
		this.shallowFlag = false;
		// Compute the code base directory.
		String baseString = System.getenv("CODE_BASE");
		if (StringUtils.isBlank(baseString)) {
			baseString = System.getProperty("user.dir");
		}
		this.baseDir = new File(baseString);
	}

	@Override
	protected boolean validateParms() throws IOException, ParseFailureException {
		// Validate the project directory.
		this.projDir = new File(this.baseDir, this.projName);
		if (! this.projDir.isDirectory()) {
			throw new FileNotFoundException("Project " + this.projName + " was not found in "
					+ this.baseDir + ".");
		}
		return true;
	}

	@Override
	protected void runCommand() throws Exception {
		// Connect to the repo.
		log.info("Connecting to git repo in {}.", this.projDir);
		try (GitRepo repo = new GitRepo(this.projDir)) {
			log.info("Pulling into {} using branch {} from remote {}.", this.projName, this.branchName,
					this.remoteName);
			// Process according to whether or not we need to pull submodules.
			if (! repo.hasSubmodules() || this.shallowFlag)
				this.singlePull(repo);
			else
				this.multiPull(repo);
		}
	}

	/**
	 * Pull the specified repository and all of its sub-modules.
	 *
	 * @param repo	repository to pull
	 *
	 * @throws ConfigInvalidException
	 * @throws IOException
	 * @throws GitAPIException
	 */
	private void multiPull(GitRepo repo) throws GitAPIException, IOException, ConfigInvalidException {
		Map<String, PullResult> resultMap = repo.pullComplete(this.remoteName, this.branchName);
		for (var resultEntry : resultMap.entrySet()) {
			String module = resultEntry.getKey();
			PullResult result = resultEntry.getValue();
			log.info("Pulled {}: {}", module, GitRepo.resultMessageFor(result));
		}
	}

	/**
	 * Pull the specified repository only.
	 *
	 * @param repo	repository to pull
	 *
	 * @throws GitAPIException
	 */
	private void singlePull(GitRepo repo) throws GitAPIException {
		PullResult result = repo.pull(this.remoteName, this.branchName);
		log.info("Pull {}", GitRepo.resultMessageFor(result));
	}

}
