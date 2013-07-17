/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.util;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.dspace.statistics.ElasticSearchLogger;

/**
 * Perform operations on Elastic Search statistics.
 * - Determine if there are robots
 * - Remove robots
 * - Optimize
 */
public class StatisticsClientElasticSearch
{
    private static final Logger log = Logger.getLogger(StatisticsClientElasticSearch.class);

    /**
     * Print the help message
     *
     * @param options The command line options the user gave
     * @param exitCode the system exit code to use
     */
    private static void printHelp(Options options, int exitCode)
    {
        // print the help message
        HelpFormatter myhelp = new HelpFormatter();
        myhelp.printHelp("StatisticsClientElasticSearch\n", options);
        System.exit(exitCode);
    }

    /**
     * Main method to run the statistics importer.
     *
     * @param args The command line arguments
     * @throws Exception If something goes wrong
     */
    public static void main(String[] args) throws Exception
    {
        CommandLineParser parser = new PosixParser();

        Options options = new Options();

        //update UA file
        //update IP file
        options.addOption("m", "mark-spiders", false, "Update isBot Flag in ES");
        options.addOption("f", "delete-spiders-by-flag", false, "Delete Spiders in ES By isBot Flag");
        options.addOption("h", "help", false, "help");

        options.addOption("i", "ip", false, "Test if robot by IP Address");
        options.addOption("d", "domain", false, "Test if robot by Domain Name");
        options.addOption("u", "useragent", false, "Test if robot by User Agent");

        CommandLine line = parser.parse(options, args);

        String[] realArgs = line.getArgs();

        // Did the user ask to see the help?
        if (line.hasOption('h'))
        {
            printHelp(options, 0);
        }

        if (line.hasOption('m'))
        {
            ElasticSearchLogger.markRobots();
        }
        else if(line.hasOption('f'))
        {
            ElasticSearchLogger.deleteRobotsByIsBotFlag();
        } else if(line.hasOption('i')) {
            if(realArgs != null && realArgs.length > 0) {
                System.out.println("IpAddress (" + realArgs[0] + ") : " + SpiderDetector.isSpiderByIP(realArgs[0]));

            } else {
                System.out.println("Specify an IP Address to check.");
            }
        } else if(line.hasOption('d')) {
            if(realArgs != null && realArgs.length > 0) {
                System.out.println("Domain (" + realArgs[0] + ") : " + SpiderDetector.isSpiderByDomainNameRegex(realArgs[0]));
            } else {
                System.out.println("Specify a Domain Name to check.");
            }
        } else if(line.hasOption('u')) {
            if(realArgs != null && realArgs.length > 0) {
                System.out.println("UserAgentSpider (" + realArgs[0] + ") : " + SpiderDetector.isSpiderByUserAgent(realArgs[0]));
            } else {
                System.out.println("Specify a User Agent to check.");
            }
        }
        else
        {
            printHelp(options, 0);
        }
    }




}
