/**
 *
 */
package org.theseed.config.git;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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
				PullResult result = repo.pull("origin", "master");
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
		CodeBase codeBase = new CodeBase(testBaseDir);
		// Build a set of the known projects.
		Set<String> PROJ_SET = Set.of("aurora.python", "brc.parent", "core.utils", "genome.survey",
				"basic", "bins.generate", "distance", "dl4j.decision", "dl4j.eval", "excel.utils",
				"genome.changes", "genome.download", "io.template", "java.config", "java.erdb",
				"kmers.hammer", "kmers.reps", "p3api", "sequence", "shared");
		// Test iteration through the code base.
		int count = 0;
		Iterator<File> iter = codeBase.new ProjectIterator();
		while (iter.hasNext()) {
			count++;
			String name = iter.next().getName();
			assertThat(name, in(PROJ_SET));
		}
		assertThat(count, equalTo(PROJ_SET.size()));
		// Create a base processor.
		// Test random access.
		for (String project : PROJ_SET) {
			try (GitRepo repo = codeBase.getRepo(project)) {
				String branch = repo.getBranch("origin");
				assertThat(project, branch, not(nullValue()));
			}
		}
	}


}