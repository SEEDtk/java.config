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
 * The positional parameter is the name of the project to update.
 *
 * The command-line options are as follows:
 *
 * -h	display command-line usage
 * -v	display more frequent log messages
 *
 * --base		module directory base (default is value of CODE_BASE (if any), else the current directory)
 * --shallow	if specified, submodules will not be pulled
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
