/**
 *
 */
package org.theseed.config.git;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bruce Parrello
 *
 */
class GitTests {

	// FIELDS
	/** logging facility */
	protected static Logger log = LoggerFactory.getLogger(GitTests.class);

	@Test
	void testCrazyStuff() throws Exception {
		File testBaseDir = new File("/Users/drake/Documents/SEEDtk/git");
		CodeBase codeBase = new CodeBase(testBaseDir);
		Map<String, String> branchMap = new TreeMap<String, String>();
		// Get branches for all the projects.
		Iterator<File> iter = codeBase.iterator();
		while (iter.hasNext()) {
			File project = iter.next();
			try (GitRepo repo = new GitRepo(project)) {
				String branch = repo.getBranch("origin");
				assertThat(branch, not(nullValue()));
				branchMap.put(project.getName(), branch);
			}
		}
		assertThat(branchMap.isEmpty(), equalTo(false));
	}


}