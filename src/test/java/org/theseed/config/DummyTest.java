/**
 *
 */
package org.theseed.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;

/**
 * This is a dummy test class to insure we don't lose the test directory in GIT.
 *
 * @author Bruce Parrello
 *
 */
class DummyTest {

	@Test
	void testObjectivism() {
		assertThat("Ayn Rand reference", "A", equalTo("A"));
	}

}
