/**
 *
 */
package org.theseed.config;

import java.io.IOException;

import org.theseed.basic.BaseProcessor;
import org.theseed.basic.ParseFailureException;

/**
 * This subcommand pulls a single GIT repo to update it to the latest version. It is used
 * to do spot fixes on existing code.
 *
 * The environment variable CODE_BASE is used to find the base directory for modules.
 *
 * @author Bruce Parrello
 *
 */
public class PullProcessor extends BaseProcessor {

	// FIELDS

	// TODO data members for PullProcessor

	// TODO constructors and methods for PullProcessor

	@Override
	protected void setDefaults() {
		// TODO code for setDefaults

	}

	@Override
	protected boolean validateParms() throws IOException, ParseFailureException {
		// TODO code for validateParms
		return false;
	}

	@Override
	protected void runCommand() throws Exception {
		// TODO code for runCommand

	}

}
