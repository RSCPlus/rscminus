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

package rscminus.scraper;

import rscminus.common.FileUtil;
import rscminus.common.JGameData;
import rscminus.common.Logger;
import rscminus.common.Settings;
import rscminus.game.PacketBuilder;
import rscminus.game.WorldManager;
import rscminus.game.constants.Game;
import rscminus.game.world.ViewRegion;
import rscminus.scraper.client.Character;

import java.io.*;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static rscminus.common.Settings.dumpNPCDamage;
import static rscminus.common.Settings.initDir;
import static rscminus.scraper.ReplayEditor.appendingToReplay;

public class Scraper {
    public static long startTime = 0;

    public static Map<Integer, Integer> m_sceneryLocs = Collections.synchronizedMap(new ConcurrentHashMap<Integer, Integer>());
    public static Map<Integer, Integer> m_boundaryLocs = Collections.synchronizedMap(new ConcurrentHashMap<Integer, Integer>());

    public static Map<Integer, String> m_replaysKeysProcessed = Collections.synchronizedMap(new ConcurrentHashMap<Integer, String>());

    public static List<String> m_npcLocCSV = Collections.synchronizedList(new ArrayList<String>());
    public static List<String> m_replayDictionarySQL = Collections.synchronizedList(new ArrayList<String>());
    public static List<String> m_chatSQL = Collections.synchronizedList(new ArrayList<String>());
    public static List<String> m_messageSQL = Collections.synchronizedList(new ArrayList<String>());
    public static List<String> m_inventorySQL = Collections.synchronizedList(new ArrayList<String>());
    public static List<String> m_damageSQL = Collections.synchronizedList(new ArrayList<String>());

    // Settings
    public static int sanitizeVersion = -1; // can set replay version (shouldn't though)
    private static StripperWindow stripperWindow;

    public static ArrayList<String> replayDirectories = new ArrayList<String>();
    public static boolean scraping = false;
    public static boolean stripping = false;
    public static int ipFoundCount = 0;
    public static int replaysProcessedCount = 0;

    public static WorldManager worldManager;

    private static void dumpScenery(String fname) {
        int sceneryCount = 0;
        for (HashMap.Entry<Integer, Integer> entry : m_sceneryLocs.entrySet()) {
            if (entry.getValue() != ScraperProcessor.SCENERY_BLANK)
                sceneryCount++;
        }
        try {
            DataOutputStream out = new DataOutputStream(new FileOutputStream(new File(fname)));
            out.writeInt(sceneryCount);
            for (HashMap.Entry<Integer, Integer> entry : m_sceneryLocs.entrySet()) {
                int x = ScraperProcessor.getPackedX(entry.getKey());
                int y = ScraperProcessor.getPackedY(entry.getKey());
                int id = entry.getValue();
                if (id != ScraperProcessor.SCENERY_BLANK) {
                    out.writeShort(x);
                    out.writeShort(y);
                    out.writeShort(id);
                }
            }
            out.close();
            Logger.Info("Dumped " + sceneryCount + " Scenery locations");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void dumpBoundaries(String fname) {
        try {
            DataOutputStream out = new DataOutputStream(new FileOutputStream(new File(fname)));
            int count = m_boundaryLocs.size();
            out.writeInt(count);
            for (HashMap.Entry<Integer, Integer> entry : m_boundaryLocs.entrySet()) {
                int x = ScraperProcessor.getPackedX(entry.getKey());
                int y = ScraperProcessor.getPackedY(entry.getKey());
                int id = ScraperProcessor.getPackedX(entry.getValue());
                int direction = ScraperProcessor.getPackedY(entry.getValue());
                out.writeShort(x);
                out.writeShort(y);
                out.writeShort(id);
                out.writeByte(direction);
            }
            out.close();
            Logger.Info("Dumped " + count + " Boundary locations");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void dumpNPCLocs(String fname) {
        try {
            Logger.Info("There's  a whopping " +  (m_npcLocCSV.size() - 1) + " NPC locations to dump. Please hold.");
            DataOutputStream out = new DataOutputStream(new FileOutputStream(new File(fname)));
            for (String entry : m_npcLocCSV) {
                out.writeBytes(entry);
            }
            out.close();
            Logger.Info("Dumped " + (m_npcLocCSV.size() - 1) + " NPC locations");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void dumpSQLToFile(List<String> sqlStatements, String identifier, String fname) {
        try {
            int size = (sqlStatements.size() - 1);
            int nullCount = 0;
            Logger.Info(String.format("@|green There's a whopping %d %s%s to dump. Please hold.|@", size, identifier, size == 1 ? "" : "s"));
            DataOutputStream out = new DataOutputStream(new FileOutputStream(new File(fname)));
            for (String entry : sqlStatements) {
                if (entry == null) {
                    nullCount++;
                    continue;
                }
                out.writeBytes(entry);
            }
            out.close();
            Logger.Info(String.format("@|green,intensity_bold Dumped %d %s sql statement%s!|@", size - nullCount, identifier, size == 1 ? "" : "s"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

        private static void printHelp(String args[]) {
        System.out.println("\nrscminus v" + Settings.versionNumber+"\n");
        System.out.println("syntax:");
        System.out.println("\t[OPTIONS] [REPLAY DIRECTORY]");
        System.out.println("options:");
        System.out.println("\t-a\t\t\tAppend client test data to the end of all replays being processed");
        System.out.println("\t-c\t\t\tDump anonymized Chat and Private Messages SQL file");
        System.out.println("\t-d\t\t\tDump scenery & boundary data to binary files");
        System.out.println("\t-f\t\t\tRemove opcodes related to the friend's list");
        System.out.println("\t-h\t\t\tShow this usage dialog");
        System.out.println("\t-m\t\t\tDump System Messages and NPC <-> Player Dialogues to SQL file");
        System.out.println("\t-n\t\t\tDump NPC locations to SQL file");
        System.out.println("\t-p\t\t\tSanitize public chat");
        System.out.println("\t-s\t\t\tExport sanitized replays");
        System.out.println("\t-v<0-" + ReplayEditor.VERSION + ">\t\t\tSet sanitizer replay version (Default is original replay version)");
        System.out.println("\t-w\t\t\tDump Sleepwords to BMP and BIN files");
        System.out.println("\t-x\t\t\tSanitize private chat");
        System.out.println("\t-z\t\t\tProcess replays even if they're not authentic");
    }

    private static boolean parseArguments(String args[]) {
        for (String arg : args) {
            switch(arg.toLowerCase().substring(0, 2)) {
                case "-a":
                    appendingToReplay = true;
                    break;
                case "-b":
                    Settings.checkBoundaryRemoval = true;
                    break;
                case "-c":
                    Settings.dumpChat = true;
                    Logger.Info("dump chat set");
                    break;
                case "-d":
                    Settings.dumpScenery = true;
                    Settings.dumpBoundaries = true;
                    Logger.Info("dumping scenery & boundaries");
                    break;
                case "-f":
                    Settings.sanitizeFriendsIgnore = true;
                    Logger.Info("sanitize Friends Ignore set");
                    break;
                case "-h":
                    return false;
                case "-i":
                    Settings.dumpInventories = true;
                    break;
                case "-m":
                    Settings.dumpMessages = true;
                    Logger.Info("dump messages set");
                    break;
                case "-n":
                    Settings.dumpNpcLocs = true;
                    Logger.Info("dump npcs locations set");
                    break;
                case "-p":
                    Settings.sanitizePublicChat = true;
                    Logger.Info("sanitize Public Chat set");
                    break;
                case "-r":
                    Settings.dumpNPCDamage = true;
                    Logger.Info("dump NPC damage sources set");
                    break;
                case "-s":
                    Settings.sanitizeReplays = true;
                    Logger.Info("sanitize Replays set");
                    break;
                case "-t":
                    Settings.threads = Integer.parseInt(arg.toLowerCase().substring(2));
                    break;
                case "-v":
                    try {
                        int version = Integer.parseInt(arg.substring(2, arg.length()));
                        if (version < 0 || version > ReplayEditor.VERSION)
                            return false;
                        sanitizeVersion = version;
                    } catch (Exception e) {
                        return false;
                    }
                    break;
                case "-w":
                    Settings.dumpSleepWords = true;
                    Logger.Info("dump sleepwords set");
                    break;
                case "-x":
                    Settings.sanitizePrivateChat = true;
                    Logger.Info("sanitize Private Chat set");
                    break;
                case "-z":
                    Settings.sanitizeForce = true;
                    Logger.Info("sanitize Force set");
                    break;
                default:
                    // Invalid argument
                    if (arg.charAt(0) == '-')
                        return false;
                    // assume that we have reached the final filepath argument
                    Settings.sanitizePath = arg;

                    // initialize info needed by each scraper method
                    if (Settings.dumpNPCDamage ||
                        Settings.checkBoundaryRemoval ||
                        Settings.dumpInventories
                    ) {
                        JGameData.init(true);
                    }

                    if (Settings.checkBoundaryRemoval) {
                        worldManager = new WorldManager();
                        worldManager.init();
                    }

                    return true;
            }
        }

        return false;
    }


    public static void findReplayDirectories(String path) {
        File[] files = new File(path).listFiles();
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f.isDirectory()) {
                String replayDirectory = f.getAbsolutePath();
                File replay = new File(replayDirectory + "/keys.bin");

                if (replay.exists()) {
                    replayDirectories.add(replayDirectory);
                } else {
                    findReplayDirectories(replayDirectory);
                }
            }
        }

    }


    public static void processDirectory(String path) {
        findReplayDirectories(path);

        ExecutorService executor = Executors.newFixedThreadPool(Settings.threads);

        for (String replayDirectory : replayDirectories) {
            Runnable sp = new ScraperProcessor(replayDirectory);
            executor.execute(sp);
        }

        executor.shutdown();
        while (!executor.isTerminated()) {
        }

        Logger.Info("@|cyan Finished processing all replays in " + ((float)(System.currentTimeMillis() - startTime) / 1000) + " seconds! |@");
    }


    public static void strip() {
        Logger.Info("Stripping " + Settings.sanitizePath);
        String filename = new File(Settings.sanitizePath).getName();
        if (filename.equals("Processing!") || filename.equals("Finished!")) {
            Logger.Warn("@|red Invalid folder asked to be stripped.|@");
            Logger.Info("@|red Hit CTRL-V to paste into the text field.|@");
            return;
        }

        stripping = true;
        Settings.sanitizePath = new File(Settings.sanitizePath).toPath().toAbsolutePath().toString();
        Settings.sanitizeBaseOutputPath = Settings.Dir.JAR + "/strippedReplays";
        Settings.scraperOutputPath = Settings.Dir.JAR + "/dump/";
        FileUtil.mkdir(Settings.scraperOutputPath);

        if (Settings.dumpSleepWords) {
            FileUtil.mkdir(Settings.scraperOutputPath + "/sleepwords");
            FileUtil.mkdir(Settings.scraperOutputPath + "/sleepwords/images");
            FileUtil.mkdir(Settings.scraperOutputPath + "/sleepwords/packetData");
        }
        Settings.sanitizeOutputPath = Settings.sanitizeBaseOutputPath;

        if (Settings.dumpNpcLocs) {
            m_npcLocCSV = new ArrayList<String>();
            m_npcLocCSV.add("replayName,timeStamp,irlTimeStamp,npcId,npcServerIndex (Not Unique!),XCoordinate,YCoordinate");
        }

        FileUtil.mkdir(Settings.sanitizePath);

        Settings.sanitizeOutputPath = Settings.sanitizeOutputPath + "/" + new File(Settings.sanitizePath).getName();
        FileUtil.mkdir(Settings.sanitizeOutputPath);
        Logger.Info("Saving to " + Settings.sanitizeOutputPath);

        File replay = new File(Settings.sanitizePath + "/in.bin.gz");
        if (replay.exists()) {
            new ScraperProcessor(Settings.sanitizePath).run();
        } else {
            processDirectory(Settings.sanitizePath);
        }
        if (Settings.dumpMessages) {
            dumpSQLToFile(m_messageSQL, "message", Settings.scraperOutputPath + "allMessages.sql");
        }
        if (Settings.dumpInventories) {
            dumpSQLToFile(m_inventorySQL, "inventory", Settings.scraperOutputPath + "allInventories.sql");
        }

        if (Settings.dumpChat) {
            dumpSQLToFile(m_chatSQL, "chat message", Settings.scraperOutputPath + "allChatMessages.sql");
        }

        if (Settings.dumpMessages || Settings.dumpChat) {
            dumpSQLToFile(m_replayDictionarySQL, "replay path", Settings.scraperOutputPath + "replayDictionary.sql");
        }

        if (Settings.dumpNpcLocs) {
            dumpNPCLocs(Settings.scraperOutputPath + "npcLocs.csv");
        }

        if (Settings.dumpScenery) {
            dumpScenery(Settings.scraperOutputPath + "scenery.bin");
        }
        if (Settings.dumpBoundaries) {
            dumpBoundaries(Settings.scraperOutputPath + "boundaries.bin");
        }

        if (Settings.dumpNPCDamage) {
            dumpSQLToFile(m_damageSQL,  "ranged damage", "rangedDataUnambiguous.sql");
        }

        Logger.Info(String.format("@|green %d out of %d replays were able to have an IP address determined.|@", ipFoundCount, replaysProcessedCount));
        Logger.Info("Saved to " + Settings.sanitizeOutputPath);
        Logger.Info("@|green,intensity_bold Finished Stripping/Optimizing!|@");
        stripping = false;
        replaysProcessedCount = 0;
        ipFoundCount = 0;
    }

    public static void main(String args[]) {
        initDir();
        Logger.Info("Dir.JAR: " + Settings.Dir.JAR);

        if (!parseArguments(args)) {
            // print help on how to use command line
            printHelp(args);

            // then initialize and show the gui
            try {
                setStripperWindow(new StripperWindow());
                stripperWindow.showStripperWindow();
            } catch (Exception e) {
            }

            return;
        } else { // command line arguments specified, don't create gui
            if (    Settings.dumpScenery ||
                    Settings.dumpBoundaries ||
                    Settings.dumpNpcLocs ||
                    Settings.dumpMessages ||
                    Settings.dumpChat ||
                    Settings.dumpSleepWords ||
                    Settings.sanitizeReplays
            ) {
                startTime = System.currentTimeMillis();
                Logger.Info(String.format("@|magenta,intensity_bold Using %d thread%s|@", Settings.threads, Settings.threads == 1 ? "" : "s"));
                strip();
            }
            return;
        }
    }


    /** @return the window */
    public static StripperWindow getStripperWindow() {
        return stripperWindow;
    }


    /** @param window the window to set */
    public static void setStripperWindow(StripperWindow window) {stripperWindow = window;}

}
