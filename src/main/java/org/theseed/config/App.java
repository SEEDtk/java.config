package org.theseed.config;

import java.util.Arrays;

import org.theseed.basic.BaseProcessor;

/**
 * This program performs SEEDtk configuration operations in pure Java. This
 * allows us to run configuration independent of the environment on Argonne
 * servers.
 *
 * The sub-commands are as follows:
 *
 * pull		update a single repo
 *
 * @author Bruce Parrello
 *
 */
public class App {

    /** static array containing command names and comments */
    protected static final String[] COMMANDS = new String[] {
            "pull", "update a single repo"
    };

    public static void main(String[] args) {

        // Get the control parameter.
        String command = args[0];
        String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
        BaseProcessor processor;
        switch (command) {
        case "pull" :
        	processor = new PullProcessor();
        	break;
        case "-h" :
        case "--help" :
            processor = null;
            break;
        default :
            throw new RuntimeException("Invalid command " + command + ".");
        }
        if (processor == null)
            BaseProcessor.showCommands(COMMANDS);
        else {
            boolean ok = processor.parseCommand(newArgs);
            if (ok) {
                processor.run();
            }
        }
    }
}
