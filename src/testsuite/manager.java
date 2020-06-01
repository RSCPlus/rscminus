/**
 * rscminus
 *
 * This file is part of rscminus.
 *
 * rscminus is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * rscminus is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with rscminus. If not,
 * see <http://www.gnu.org/licenses/>.
 *
 * Authors: see <https://github.com/OrN/rscminus>
 */

package testsuite;

import testsuite.types.unitTest;
import java.util.ArrayList;
import java.util.List;


public class manager{
    private static boolean help = false;
    public static String sqlUsername = null;
    public static String sqlPassword = null;
    private static List<String> testsToRun = new ArrayList<>();

    private static Argument[] argumentList = new Argument[] {
            new Argument("help", "shows all available arguments and their syntax"),
            new Argument("runall", "runs all available tests"),
            new Argument("run", "runs a single test"),
    };

    public static void main(String[] args) {
        processArguments(args);

        if (help) {
            System.out.println("[RSC- Test Suite]");
            System.out.println("Usage: java -jar rscminus-testsuite.jar [arguments]");
            System.out.println("Available arguments:");
            for (Argument argument : argumentList)
                System.out.println(String.format("-%-8s", argument.name) + ": " + argument.explanation);
        } else {
            if (!testsToRun.isEmpty()) {
                for (String testName : testsToRun) {
                    System.out.println("------------------------------");
                    try {
                        Class<?> requestedClass = Class.forName("testsuite.tests.".concat(testName));
                        Object requestedObject = requestedClass.newInstance();
                        if (requestedObject instanceof unitTest) {
                            unitTest test = ((unitTest) requestedObject);
                            try {
                                System.out.println(testName.concat(" found. Initialising..."));
                                if (((unitTest) requestedObject).init()) {
                                    System.out.println("Complete. Running ...");
                                    if (((unitTest) requestedObject).run()) {
                                        System.out.println(testName.concat(" passed"));
                                    } else
                                        System.out.println(testName.concat(" failed"));
                                } else
                                    System.out.println(testName.concat(" couldn't be ran."));
                            } finally {
                                test.cleanup();
                            }
                        }
                    } catch (ClassNotFoundException a) {
                        System.out.println(testName.concat(" not found"));
                    } catch (Exception b) {
                        System.out.println(testName.concat(" failed due to exception: "));
                        b.printStackTrace();
                    }
                }
            }
        }
    }

    private static void processArguments(String[] args) {
        if (args.length == 0) {
            help = true;
            return;
        }

        for (int argumentIndex=0; argumentIndex < args.length; ++argumentIndex) {
            if (args[argumentIndex].equalsIgnoreCase("-help")) {
                help = true;
                return;
            }
            else if (args[argumentIndex].equalsIgnoreCase("-run"))
                testsToRun.add(args[++argumentIndex]);
            else if (args[argumentIndex].equalsIgnoreCase("-sqlu")) {
                sqlUsername = args[++argumentIndex];
            }
            else if (args[argumentIndex].equalsIgnoreCase("-sqlp")) {
                sqlPassword = args[++argumentIndex];
            }
            else {
                System.out.println("Unknown argument " + args[argumentIndex]);
                testsToRun.clear();
                return;
            }
        }
    }

    public static boolean promptContinue(String message) {
        char c = '\0';
        try {
            do {
                System.out.println(message.concat(" [y/n]"));
                c = (char)System.in.read();
                while (System.in.available() > 0)
                    System.in.read();
            } while(c != 'y' && c != 'n');
        } catch (Exception a) {
            a.printStackTrace();
            return false;
        }
        return c == 'y';
    }
    private static class Argument {
        String name;
        String explanation;

        public Argument(String name, String explanation) {
            this.name = name;
            this.explanation = explanation;
        }
    }
}
