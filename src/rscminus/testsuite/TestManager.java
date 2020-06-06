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

package rscminus.testsuite;

import rscminus.testsuite.types.UnitTest;

import java.io.File;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;


public class TestManager {
    private static boolean help = false;
    public static String sqlUsername = null;
    public static String sqlPassword = null;
    private static List<String> testsToRun = new ArrayList<>();

    private static Argument[] argumentList = new Argument[] {
            new Argument("help", "shows all available arguments (you are viewing this now)"),
            new Argument("sqlu", "accepts your local sql username. eg -sqlu root"),
            new Argument("sqlp", "accepts your local sql password. eg -sqlp root"),
            new Argument("runall", "runs all available tests"),
            new Argument("run", "runs a single test. eg: -run test1 -run test2"),
    };

    private static String testPackage = "rscminus.testsuite.tests.";

    public static void main(String[] args) {
        processArguments(args);
        if (help) {
            System.out.println("[RSC- Test Suite]");
            System.out.println("Usage: java -jar rscminus-rscminus.testsuite.jar [arguments]");
            System.out.println("Available arguments:");
            for (Argument argument : argumentList)
                System.out.println(String.format("-%-8s", argument.name) + ": " + argument.explanation);
        } else {
            if (!testsToRun.isEmpty()) {
                for (String testName : testsToRun) {
                    System.out.println("------------------------------");
                    try {
                        Class<?> requestedClass = Class.forName(
                                testName.startsWith(testPackage) ? testName : testPackage.concat(testName)
                        );
                        Object requestedObject = requestedClass.newInstance();
                        if (requestedObject instanceof UnitTest) {
                            UnitTest test = ((UnitTest) requestedObject);
                            try {
                                System.out.println(testName.concat(" found. Initialising..."));
                                if (((UnitTest) requestedObject).init()) {
                                    System.out.println("Complete. Running ...");
                                    if (((UnitTest) requestedObject).run()) {
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
            else if (args[argumentIndex].equalsIgnoreCase("-runall")) {
                try {
                    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                    assert classLoader != null;
                    Enumeration<URL> resources = classLoader.getResources(testPackage.replace('.', '/'));
                    while (resources.hasMoreElements()) {
                        URL resource = resources.nextElement();
                        if (resource.toURI().getScheme().equals("jar")) {
                            testsToRun.addAll(
                                findClasses(resource, testPackage)
                            );
                        } else {
                            testsToRun.addAll(
                                findClasses(new File(resource.getFile()), testPackage)
                            );
                        }
                    }
                } catch (Exception a) {a.printStackTrace();}
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
    private static List<String> findClasses(URL url, String packageName) throws Exception {
        List<String> classes = new ArrayList<>();
        FileSystem fileSystem = FileSystems.newFileSystem(url.toURI(), Collections.emptyMap());
        Path myPath = fileSystem.getPath(testPackage.replace('.', '/'));
        Stream<Path> walk = Files.walk(myPath);
        for (Iterator<Path> it = walk.iterator(); it.hasNext(); ) {
            String path = it.next().getFileName().toString();
            if (path.endsWith(".class")) {
                classes.add(path.substring(0, path.length() - 6));
            }
        }
        return classes;
    }
    private static List<String> findClasses(File directory, String packageName) {
        List<String> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        for (File file : files) {
            System.out.println(file.getName());
            if (file.isDirectory()) {
                assert !file.getName().contains(".");
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                classes.add(file.getName().substring(0, file.getName().length() - 6));
            }
        }
        return classes;
    }
}
