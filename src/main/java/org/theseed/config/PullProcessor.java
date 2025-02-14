/**
 *
 */
package org.theseed.config;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.eclipse.jgit.api.PullResult;
import org.kohsuke.args4j.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.ParseFailureException;
import org.theseed.config.git.BaseGitProcessor;
import org.theseed.config.git.GitRepo;

/**
 * This method will pull all the repos from a code base. It does a simple pull from the
 * specified origin with no frills. The only advantage is finding all the GIT projects
 * in the specified directory.
 *
 * The positional parameter is the name of the remote origin.
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
public class PullProcessor extends BaseGitProcessor {

	// FIELDS
	/** logging facility */
	protected static Logger log = LoggerFactory.getLogger(PullProcessor.class);

	// COMMAND-LINE OPTIONS

	/** name of the remote origin */
	@Argument(index = 0, metaVar = "origin", usage = "name of the remote origin", required = true)
	private String remoteName;


	@Override
	protected void setGitDefaults() {
	}

	@Override
	protected void validateGitParms() throws ParseFailureException, IOException {
	}

	@Override
	protected void runGitCommand() throws Exception {
		// Get access to all the repos in the code base.
		Iterator<File> iter = this.getRepos();
		while (iter.hasNext()) {
			File projFile = iter.next();
			log.info("Processing project in {}.", projFile);
			try (GitRepo repo = new GitRepo(projFile)) {
				PullResult result = repo.pull(this.remoteName, null);
				log.info("Result for {}: {}", repo, GitRepo.resultMessageFor(result));
			}
		}

	}

}
