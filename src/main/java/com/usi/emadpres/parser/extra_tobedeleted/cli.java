package com.usi.emadpres.parser.extra_tobedeleted;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.usi.emadpres.parser.extra_tobedeleted.JavaAPIAnalyzer.parseFiles;

@Deprecated //TODO: Make me a useful CLI for analyzing projects, ready to be passed to another student. Not for ICSE21.
public class cli {
    private static final Logger logger = LoggerFactory.getLogger(cli.class);

    // CLI: command line interface
    private static final Options options = new Options();
    private static final String OPT_HELP = "help";
    private static final String OPT_PROJECT = "project";
    private static final String OPT_CLASSPATH = "classpath";

    public static void main(String[] args) {
        CreateOptions();
        CommandLineParser parser = new DefaultParser(); // create the parser
        CommandLine cmdLine; // parse the command line arguments


        try {
            cmdLine = parser.parse(options, args);
        } catch (ParseException e) {
            logger.error("Bad commandline usage: " + e);
            PrintHelp();
            return;
        }

        String[] dirs = cmdLine.getArgs(); // directories to be parsed and analyzed
        if (cmdLine.hasOption(OPT_HELP) || dirs.length < 1) {
            PrintHelp();
            return;
        }

        String classpath = "";
        if (cmdLine.hasOption(OPT_CLASSPATH)) {
            classpath = cmdLine.getOptionValue(OPT_CLASSPATH);
        }

        for (String dir : dirs) {
            logger.info("Parse directory: " + dir);
            parseFiles(dir, classpath.split(":"));
            logger.info("Parse directory done.");
        }
    }

    private static void CreateOptions() {
        final Option help = new Option(OPT_HELP, "Print this message. ");
        final Option project = new Option(OPT_PROJECT, "Project name.");
        final Option classpath = new Option(OPT_CLASSPATH, "Classpath (colon separated).");

        options.addOption(help);
        options.addOption(project);
        options.addOption(classpath);
    }

    private static void PrintHelp() {
        // automatically generate the help statement
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(cli.class.getSimpleName() + " [options] dir1 [dir2]...", options);
    }
}
