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
import rscminus.common.Sleep;
import rscminus.game.PacketBuilder;
import rscminus.game.constants.Game;
import rscminus.game.world.ViewRegion;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.LinkedList;

public class Scraper {
    private static HashMap<Integer, Integer> m_objects = new HashMap<Integer, Integer>();
    private static HashMap<Integer, Integer> m_wallObjects = new HashMap<Integer, Integer>();

    private static final int OBJECT_BLANK = 65536;

    // Settings
    private static String sanitizePath = "replays";
    private static String sanitizeOutputPath = "output";
    private static int sanitizeVersion = -1;
    private static boolean sanitizeReplays = false;
    private static boolean sanitizePublicChat = false;
    private static boolean sanitizePrivateChat = false;
    private static boolean sanitizeFriendsIgnore = false;

    private static boolean objectIDBlacklisted(int id, int x, int y) {
        boolean blacklist = false;
        if (id == 1147) // Spellcharge
            blacklist = true;
        else if (id == 1142) // clawspell
            blacklist = true;
        else if (id == 490) // Tree (Mithril Seeds)
            blacklist = true;
        else if (id == 1031) // Lightning
            blacklist = true;
        else if (id == 830) // Flames of zamorak
            blacklist = true;
        else if (id == 946) // dwarf multicannon base
            blacklist = true;
        else if (id == 947) // dwarf multicannon stand
            blacklist = true;
        else if (id == 948) // dwarf multicannon barrels
            blacklist = true;
        else if (id == 943) // dwarf multicannon base
            blacklist = true;
        else if (id == 1036) // Flames
            blacklist = true;
        else if (id == 1071) // Leak
            blacklist = true;
        else if (id == 1077) // Leak
            blacklist = true;

        if (blacklist)
            System.out.println("GameObject id " + id + " at " + x + ", " + y + " was blacklisted");

        return blacklist;
    }

    private static boolean objectIDRemoveList(int id, int x, int y) {
        boolean remove = false;

        if (id == 97) // fire
            remove = true;

        if (remove)
            System.out.println("GameObject id " + id + " at " + x + ", " + y + " was removed");

        return remove;
    }

    private static boolean wallObjectIDBlacklisted(int id, int x, int y) {
        boolean blacklist = false;

        if (blacklist)
            System.out.println("WallObject id " + id + " at " + x + ", " + y + " was blacklisted");

        return blacklist;
    }

    private static int handleObjectIDConflict(int before, int after) {
        if (before == OBJECT_BLANK)
            return after;

        if (before == after)
            return before;

        if (after == 4) // Treestump
            return before;
        else if (before == 4)
            return after;

        if (after == 1087) // Jungle tree stump
            return before;
        else if (before == 1087)
            return after;

        if (after == 314) // Large treestump
            return before;
        else if (before == 314)
            return after;

        System.out.println("unhandled GameObject conflict; before: " + before + ", after: " + after);

        return before;
    }

    private static int handleObjectIDConvert(int id) {
        if (id == 63) // doors
            id = 64;
        else if (id == 203) // Coffin
            id = 202;
        else if (id == 58) // gate
            id = 57;
        else if (id == 59) // gate
            id = 60;
        else if (id == 40) // Coffin
            id = 39;
        else if (id == 63) // doors
            id = 64;
        else if (id == 71) // cupboard
            id = 56;
        else if (id == 17) // Chest
            id = 18;
        else if (id == 136) // Chest
            id = 135;
        else if (id == 79) // manhole
            id = 78;
        else if (id == 141) // cupboard
            id = 140;

        return id;
    }

    private static int handleWallObjectIDConvert(int value) {
        int id = getPackedX(value);
        int direction = getPackedY(value);

        if (id == 1) // Doorframe
            id = 2;
        else if (id == 9) // Doorframe
            id = 8;

        return packCoordinate(id, direction);
    }

    private static int handleWallObjectIDConflict(int before, int after) {
        if (before == after)
            return before;

        int beforeID = getPackedX(before);
        int beforeDirection = getPackedY(before);
        int afterID = getPackedX(after);
        int afterDirection = getPackedY(after);

        if (beforeID == 24) // Web
            return packCoordinate(beforeID, beforeDirection);
        else if (afterID == 24) // Web
            return packCoordinate(afterID, afterDirection);
        if (beforeID == 11) // Doorframe
            return packCoordinate(afterID, afterDirection);
        else if (afterID == 11)
            return packCoordinate(beforeID, beforeDirection);

        System.out.println("unhandled WallObject conflict; before: " + beforeID + ", after: " + afterID);

        return before;
    }

    private static void dumpObjects(String fname) {
        int objectCount = 0;
        for (HashMap.Entry<Integer, Integer> entry : m_objects.entrySet()) {
            if (entry.getValue() != OBJECT_BLANK)
                objectCount++;
        }
        try {
            DataOutputStream out = new DataOutputStream(new FileOutputStream(new File(fname)));
            out.writeInt(objectCount);
            for (HashMap.Entry<Integer, Integer> entry : m_objects.entrySet()) {
                int x = getPackedX(entry.getKey());
                int y = getPackedY(entry.getKey());
                int id = entry.getValue();
                if (id != OBJECT_BLANK) {
                    out.writeShort(x);
                    out.writeShort(y);
                    out.writeShort(id);
                }
            }
            out.close();
            System.out.println("Dumped " + objectCount + " objects");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void dumpWallObjects(String fname) {
        try {
            DataOutputStream out = new DataOutputStream(new FileOutputStream(new File(fname)));
            int count = m_wallObjects.size();
            out.writeInt(count);
            for (HashMap.Entry<Integer, Integer> entry : m_wallObjects.entrySet()) {
                int x = getPackedX(entry.getKey());
                int y = getPackedY(entry.getKey());
                int id = getPackedX(entry.getValue());
                int direction = getPackedY(entry.getValue());
                out.writeShort(x);
                out.writeShort(y);
                out.writeShort(id);
                out.writeByte(direction);
            }
            out.close();
            System.out.println("Dumped " + count + " wall objects");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int packCoordinate(int x, int y) {
        return ((x & 0xFFFF) << 16) | (y & 0xFFFF);
    }

    private static int getPackedX(int value) {
        return (value >> 16) & 0xFFFF;
    }

    private static int getPackedY(int value) {
        return value & 0xFFFF;
    }

    private static boolean validCoordinates(int x, int y) {
        if (x < 0 || y < 0)
            return false;

        int viewX = x >> 3;
        int width = Game.WORLD_WIDTH >> 3;
        if (viewX >= width)
            return false;
        int height = Game.WORLD_HEIGHT >> 3;
        int viewY = y >> 3;
        if (viewY >= height)
            return false;
        return true;
    }

    private static void fillView(int playerX, int playerY, HashMap<Integer, Integer> objects) {
        int viewX = (playerX >> 3) << 3;
        int viewY = (playerY >> 3) << 3;
        int size = ViewRegion.VIEW_DISTANCE << 3;
        int index = (ViewRegion.VIEW_DISTANCE / 2) << 3;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                int updateX = viewX + (x - index);
                int updateY = viewY + (y - index);
                int key = packCoordinate(updateX, updateY);
                if (!objects.containsKey(key))
                    objects.put(key, OBJECT_BLANK);
            }
        }
    }

    private static void sanitizeReplay(String fname) {
        System.out.println(fname);

        ReplayEditor editor = new ReplayEditor();
        boolean success = editor.importData(fname);

        if (!success) {
            System.out.println("Replay is not valid, skipping");
            return;
        }

        System.out.println("client version: " + editor.getReplayVersion().clientVersion);
        System.out.println("replay version: " + editor.getReplayVersion().version);

        // Process incoming packets
        LinkedList<ReplayPacket> incomingPackets = editor.getIncomingPackets();
        for (ReplayPacket packet : incomingPackets) {
            try {
                switch (packet.opcode) {
                    case ReplayEditor.VIRTUAL_OPCODE_CONNECT:
                        System.out.println("loginresponse: " + packet.data[0] + " (timestamp: " + packet.timestamp + ")");
                        break;
                    case PacketBuilder.OPCODE_UPDATE_PLAYERS: {
                        int originalPlayerCount = packet.readUnsignedShort();
                        int playerCount = originalPlayerCount;
                        for (int i = 0; i < originalPlayerCount; i++) {
                            int startPosition = packet.tell();
                            int pid = packet.readUnsignedShort();
                            int updateType = packet.readUnsignedByte();
                            if (updateType == 0) {
                                packet.skip(2);
                            } else if (updateType == 1) {
                                packet.skip(1);
                                packet.readRSCString();

                                // Strip Chat
                                if (sanitizePublicChat) {
                                    int trimSize = packet.tell() - startPosition;
                                    packet.skip(-trimSize);
                                    packet.trim(trimSize);
                                    playerCount--;
                                    continue;
                                }
                            } else if (updateType == 2) {
                                packet.skip(3);
                            } else if (updateType == 3) {
                                packet.skip(4);
                            } else if (updateType == 4) {
                                packet.skip(4);
                            } else if (updateType == 5) {
                                packet.skip(2);
                                packet.readPaddedString();
                                packet.readPaddedString();
                                int equipCount = packet.readUnsignedByte();
                                packet.skip(equipCount);
                                packet.skip(6);
                            } else if (updateType == 6) {
                                packet.readRSCString();
                            } else {
                                packet.skip(2);
                                packet.readPaddedString();
                                packet.readPaddedString();
                                packet.skip(6 + packet.readUnsignedByte());
                            }
                        }

                        // Rewrite player count
                        if (originalPlayerCount != playerCount) {
                            if (playerCount == 0) {
                                packet.opcode = ReplayEditor.VIRTUAL_OPCODE_NOP;
                            } else {
                                packet.seek(0);
                                packet.writeUnsignedShort(playerCount);
                            }
                        }
                        break;
                    }
                    case PacketBuilder.OPCODE_SET_IGNORE:
                    case PacketBuilder.OPCODE_UPDATE_IGNORE:
                    case PacketBuilder.OPCODE_UPDATE_FRIEND:
                        if (sanitizeFriendsIgnore)
                            packet.opcode = ReplayEditor.VIRTUAL_OPCODE_NOP;
                        break;
                    case PacketBuilder.OPCODE_RECV_PM:
                    case PacketBuilder.OPCODE_SEND_PM:
                        if (sanitizePrivateChat)
                            packet.opcode = ReplayEditor.VIRTUAL_OPCODE_NOP;
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Process outgoing packets
        LinkedList<ReplayPacket> outgoingPackets = editor.getOutgoingPackets();
        for (ReplayPacket packet : outgoingPackets) {
            try {
                switch (packet.opcode) {
                    case ReplayEditor.VIRTUAL_OPCODE_CONNECT: // Login
                        System.out.println("outgoing login (timestamp: " + packet.timestamp + ")");
                        break;
                    case 216: // Send chat message
                        if (sanitizePublicChat)
                            packet.opcode = ReplayEditor.VIRTUAL_OPCODE_NOP;
                        break;
                    case 218: // Send private message
                        if (sanitizePrivateChat)
                            packet.opcode = ReplayEditor.VIRTUAL_OPCODE_NOP;
                        break;
                    case 167: // Remove friend
                    case 195: // Add friend
                    case 241: // Remove ignore
                    case 132: // Add ignore
                        if (sanitizeFriendsIgnore)
                            packet.opcode = ReplayEditor.VIRTUAL_OPCODE_NOP;
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (sanitizeReplays) {
            // Set exported replay version
            if (sanitizeVersion != -1)
                editor.getReplayVersion().version = sanitizeVersion;
            String outDir = sanitizeOutputPath + fname.substring(sanitizePath.length());
            outDir = new File(outDir).toPath().toAbsolutePath().toString();
            FileUtil.mkdir(outDir);
            editor.exportData(outDir);
        }
    }

    private static void scrapeReplay(String fname) {
        Replay replay = new Replay();
        replay.load(fname);

        System.out.println(fname);

        if (!replay.isValid()) {
            System.out.println("Failed to load replay; Aborting");
            return;
        }

        HashMap<Integer, Integer> objects = new HashMap<Integer, Integer>();
        HashMap<Integer, Integer> wallObjects = new HashMap<Integer, Integer>();
        int playerX = -1;
        int playerY = -1;
        int planeX = -1;
        int planeY = -1;
        int floor = -1;
        int y_offset = -1;
        boolean loggedIn = false;

        while (!replay.isEOF()) {
            if (replay.available() < 2)
                break;
            int length = replay.readPacketLength();

            if (length == 0) {
                loggedIn = false;
                continue;
            }

            if (replay.available() < length)
                break;

            int opcode = replay.readUnsignedByte();
            length--;

            switch (opcode) {
                case PacketBuilder.OPCODE_FLOOR_SET:
                    replay.skip(2);
                    planeX = replay.readUnsignedShort();
                    planeY = replay.readUnsignedShort();
                    floor = replay.readUnsignedShort();
                    y_offset = replay.readUnsignedShort();
                    break;
                case PacketBuilder.OPCODE_CREATE_PLAYERS:
                    replay.startBitmask();
                    playerX = replay.readBitmask(11);
                    playerY = replay.readBitmask(13);
                    replay.endBitmask();
                    loggedIn = true;
                    fillView(playerX, playerY, objects);
                    replay.skip(length - 3);
                    break;
                case PacketBuilder.OPCODE_OBJECT_HANDLER:
                    while (length > 0) {
                        if (replay.readUnsignedByte() == 255) {
                            replay.skip(2);
                            length -= 3;
                        } else {
                            replay.skip(-1);
                            int type = handleObjectIDConvert(replay.readUnsignedShort());
                            int x = playerX + replay.readByte();
                            int y = playerY + replay.readByte();
                            length -= 4;

                            if (!loggedIn || planeX != Game.WORLD_PLANE_X || planeY != Game.WORLD_PLANE_Y || y_offset != Game.WORLD_Y_OFFSET || floor > 3 || floor < 0) {
                                System.out.println("Invalid region or not logged in; Aborting");
                                return;
                            }

                            if (!validCoordinates(x, y)) {
                                System.out.println("Invalid coordinates " + x + ", " + y + "; Aborting");
                                return;
                            } else if (type != 60000 && !objectIDBlacklisted(type, x, y)) {
                                if (type < 0 || type > 1188) {
                                    System.out.println("GameObject id " + type + " at " + x + ", " + y + " is invalid; Aborting");
                                    return;
                                }

                                int key = packCoordinate(x, y);
                                //System.out.println("x: " + x + ", y: " + y);
                                if (objects.containsKey(key))
                                    type = handleObjectIDConflict(objects.get(key), type);
                                objects.put(key, type);
                            }
                        }
                    }
                    break;
                case PacketBuilder.OPCODE_WALLOBJECT_HANDLER:
                    while (length > 0) {
                        if (replay.readUnsignedByte() == 255) {
                            replay.skip(2);
                            length -= 3;
                        } else {
                            replay.skip(-1);
                            int type = replay.readUnsignedShort();
                            int x = playerX + replay.readByte();
                            int y = playerY + replay.readByte();
                            byte direction = replay.readByte();
                            length -= 5;

                            if (!loggedIn || planeX != Game.WORLD_PLANE_X || planeY != Game.WORLD_PLANE_Y || y_offset != Game.WORLD_Y_OFFSET || floor > 3 || floor < 0) {
                                System.out.println("Invalid region or not logged in; Aborting");
                                return;
                            }

                            if (!validCoordinates(x, y)) {
                                System.out.println("Invalid coordinates " + x + ", " + y + "; Aborting");
                                return;
                            } else if (type != 0xFFFF && !wallObjectIDBlacklisted(type, x, y)) {
                                if (type < 0 || type > 213) {
                                    System.out.println("WallObject id " + type + " at " + x + ", " + y + " is invalid; Aborting");
                                    return;
                                }

                                int key = packCoordinate(x, y);
                                int value = handleWallObjectIDConvert(packCoordinate(type, direction));
                                if (wallObjects.containsKey(key))
                                    value = handleWallObjectIDConflict(wallObjects.get(key), value);
                                wallObjects.put(key, value);
                            }
                        }
                    }
                    break;
                default:
                    replay.skip(length);
                    break;
            }
        }

        for (HashMap.Entry<Integer, Integer> entry : objects.entrySet()) {
            int key = entry.getKey();
            int id = entry.getValue();
            if (m_objects.containsKey(key)) {
                int oldID = m_objects.get(key);
                if (oldID == OBJECT_BLANK)
                    continue;
                if (id == OBJECT_BLANK && oldID != OBJECT_BLANK && objectIDRemoveList(oldID, getPackedX(key), getPackedY(key))) {
                    m_objects.put(key, id);
                    continue;
                }
                if (id != OBJECT_BLANK) {
                    id = handleObjectIDConflict(m_objects.get(key), id);
                    m_objects.put(key, id);
                }
            } else {
                m_objects.put(key, id);
            }
        }
        for (HashMap.Entry<Integer, Integer> entry : wallObjects.entrySet()) {
            int key = entry.getKey();
            int value = entry.getValue();
            if (m_wallObjects.containsKey(key))
                value = handleWallObjectIDConflict(m_wallObjects.get(key), value);
            m_wallObjects.put(key, value);
        }
    }

    private static void sanitizeDirectory(String path) {
        File[] files = new File(path).listFiles();
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f.isDirectory()) {
                String replayDirectory = f.getAbsolutePath();
                File replay = new File(replayDirectory + "/in.bin.gz");
                if (replay.exists())
                    sanitizeReplay(replayDirectory);
                else
                    sanitizeDirectory(replayDirectory);
            }
        }
    }

    private static void scrapeDirectory(String path) {
        File[] files = new File(path).listFiles();
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f.isDirectory()) {
                String replayDirectory = f.getAbsolutePath();
                File replay = new File(replayDirectory + "/in.bin.gz");
                if (replay.exists())
                    scrapeReplay(replayDirectory);
                else
                    scrapeDirectory(replayDirectory);
            }
        }
    }

    private static void printHelp(String args[]) {
        System.out.println("syntax:");
        System.out.println("\t[OPTIONS] [REPLAY DIRECTORY]");
        System.out.println("options:");
        System.out.println("\t-h\t\t\tShow this usage dialog");
        System.out.println("\t-s\t\t\tSanitize replays");
        System.out.println("\t-p\t\t\tSanitize public chat");
        System.out.println("\t-x\t\t\tSanitize private chat");
        System.out.println("\t-f\t\t\tSanitize friends and ignores");
        System.out.println("\t-v<0-" + ReplayEditor.VERSION + ">\t\tSet sanitizer replay version");
    }

    private static boolean parseArguments(String args[]) {
        for (String arg : args) {
            switch(arg.toLowerCase().substring(0, 2)) {
                case "-h":
                    return false;
                case "-s":
                    sanitizeReplays = true;
                    break;
                case "-p":
                    sanitizePublicChat = true;
                    break;
                case "-x":
                    sanitizePrivateChat = true;
                    break;
                case "-f":
                    sanitizeFriendsIgnore = true;
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
                default:
                    // Invalid argument
                    if (arg.charAt(0) == '-')
                        return false;
                    sanitizePath = arg;
                    break;
            }
        }
        return true;
    }

    public static void main(String args[]) {
        boolean success = parseArguments(args);
        if (!success) {
            printHelp(args);
            return;
        }

        sanitizePath = new File(sanitizePath).toPath().toAbsolutePath().toString();
        sanitizeOutputPath = new File(sanitizeOutputPath).toPath().toAbsolutePath().toString();
        FileUtil.mkdir(sanitizePath);
        FileUtil.mkdir(sanitizeOutputPath);

        // Begin sanitizing
        sanitizeDirectory(sanitizePath);

        // Scrape directory
        // TODO: Combine this with our sanitizer
        scrapeDirectory(sanitizePath);
        dumpObjects(sanitizeOutputPath + "/objects.bin");
        dumpWallObjects(sanitizeOutputPath + "/wallobjects.bin");

        return;
    }
}
