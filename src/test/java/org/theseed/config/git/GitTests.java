/**
 *
 */
package org.theseed.config.git;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.ParseFailureException;

/**
 * @author Bruce Parrello
 *
 */
class GitTests {

	// FIELDS
	/** logging facility */
	protected static Logger log = LoggerFactory.getLogger(GitTests.class);

	@Test
	void testPullStuff() throws IOException, GitAPIException, ParseFailureException, ConfigInvalidException {
		// Insure we have a SEEDtk environment. Otherwise, this test will crash horribly.
		String baseDir = System.getenv("CODE_BASE");
		if (baseDir == null)
			throw new ParseFailureException("CODE_BASE not defined.");
		File seedtkBase = new File(baseDir);
		File parentProject = new File(seedtkBase, "brc.parent");
		if (! parentProject.isDirectory())
			log.warn("SEEDtk project not found. GIT tests skipped.");
		else {
			try (GitRepo repo = new GitRepo(parentProject)) {
				// Test submodule status.
				assertThat(repo.hasSubmodules(), equalTo(true));
				// Do a normal pull.
				PullResult result = repo.pull("origin");
				assertThat(result.isSuccessful(), equalTo(true));
				// Do a pull of all submodules with the top one.
				Map<String, PullResult> allResults = repo.pullComplete("origin", "master");
				assertThat(allResults.size(), greaterThan(1));
				for (var subEntry : allResults.entrySet()) {
					String name = subEntry.getKey();
					PullResult subResult = subEntry.getValue();
					assertThat(name, subResult.isSuccessful(), equalTo(true));
				}
			}
		}

	}

	// TODO: delete this test after it works
	@Test
	void testCrazyStuff() throws Exception {
		File testBaseDir = new File("/Users/drake/Documents/SEEDtk/Data/test_for_git");
		File parentProject = new File(testBaseDir, "brc.parent");
		try (GitRepo repo = new GitRepo(parentProject)) {
			// Test submodule status.
			assertThat(repo.hasSubmodules(), equalTo(true));
			// Do a pull of all submodules with the top one.
			Map<String, PullResult> allResults = repo.pullComplete("origin", "master");
			assertThat(allResults.size(), greaterThan(1));
			for (var subEntry : allResults.entrySet()) {
				String module = subEntry.getKey();
				PullResult subResult = subEntry.getValue();
				assertThat(module, subResult.isSuccessful(), equalTo(true));
				String msg = GitRepo.resultMessageFor(subResult);
				log.info("Messages for module {}: {}", module, msg);
			}
		}
	}

}