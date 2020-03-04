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
import rscminus.common.Logger;
import rscminus.common.Settings;
import rscminus.game.PacketBuilder;
import rscminus.game.constants.Game;
import rscminus.game.world.ViewRegion;

import java.awt.*;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;

import static rscminus.common.Settings.initDir;
import static rscminus.scraper.ReplayEditor.appendingToReplay;

public class Scraper {
    private static HashMap<Integer, Integer> m_objects = new HashMap<Integer, Integer>();
    private static HashMap<Integer, Integer> m_wallObjects = new HashMap<Integer, Integer>();
    private static HashMap<Integer, String> m_npcLocCSV = new HashMap<Integer, String>();

    private static final int OBJECT_BLANK = 65536;

    // Settings
    private static int sanitizeVersion = -1;
    public static int ip_address1 = 0;
    public static int ip_address2 = 0;
    public static int ip_address3 = 0xFFFF; //IPv4-mapped IPv6 addresses dictate this, 0:0:0:0:0:FFFF::/96; rfc4291 section 2.5.5.2
    public static int ip_address4 = -1;
    public static int world_num_excluded = 0;

    private static StripperWindow stripperWindow;

    public static boolean scraping = false;
    public static boolean stripping = false;
    public static int ipFoundCount = 0;
    public static int replaysProcessedCount = 0;

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
            Logger.Info("Dumped " + objectCount + " objects");
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
            Logger.Info("Dumped " + count + " wall objects");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void dumpNPCLocs(String fname) {
        try {
            Logger.Info("There's  a whopping " +  (m_npcLocCSV.size() - 1) + " NPC locs to dump. Please hold.");
            DataOutputStream out = new DataOutputStream(new FileOutputStream(new File(fname)));
            for (HashMap.Entry<Integer, String> entry : m_npcLocCSV.entrySet()) {
                out.writeBytes(entry.getValue());
            }
            out.close();
            Logger.Info("Dumped " + (m_npcLocCSV.size() - 1) + " NPC locs");
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

    private static void setFlags(ReplayEditor editor) {
        byte[] metadata = editor.getMetadata();
        if (Settings.sanitizePublicChat)
            metadata[ReplayEditor.METADATA_FLAGS_OFFSET] |= ReplayEditor.FLAG_SANITIZE_PUBLIC;
        if (Settings.sanitizePrivateChat)
            metadata[ReplayEditor.METADATA_FLAGS_OFFSET] |= ReplayEditor.FLAG_SANITIZE_PRIVATE;
        if (Settings.sanitizeFriendsIgnore)
            metadata[ReplayEditor.METADATA_FLAGS_OFFSET] |= ReplayEditor.FLAG_SANITIZE_FRIENDSIGNORES;
        if (sanitizeVersion != -1 && editor.getReplayVersion().version != sanitizeVersion)
            metadata[ReplayEditor.METADATA_FLAGS_OFFSET] |= ReplayEditor.FLAG_SANITIZE_VERSION;
    }

    private static void sanitizeReplay(String fname) {
        ReplayEditor editor = new ReplayEditor();
        setFlags(editor);
        boolean success = editor.importData(fname);

        if (!success) {
            Logger.Warn("Replay is not valid, skipping");
            return;
        }

        Logger.Info("client version: " + editor.getReplayVersion().clientVersion);
        Logger.Info("replay version: " + editor.getReplayVersion().version);

        if (!Settings.sanitizeForce && !editor.authenticReplay()) {
            Logger.Warn("Replay is not an authentic rsc replay");
            return;
        }

        Logger.Debug(fname);

        int playerX = -1;
        int playerY = -1;
        int planeX = -1;
        int planeY = -1;
        int planeFloor = -1;
        int floorXOffset = -1;

        // Process incoming packet
        LinkedList<ReplayPacket> incomingPackets = editor.getIncomingPackets();
        for (ReplayPacket packet : incomingPackets) {
            Logger.Debug(String.format("incoming opcode: %d",packet.opcode));
            try {

                switch (packet.opcode) {
                    case ReplayEditor.VIRTUAL_OPCODE_CONNECT:
                        Logger.Info("loginresponse: " + packet.data[0] + " (timestamp: " + packet.timestamp + ")");
                        break;
                    case PacketBuilder.OPCODE_FLOOR_SET:
                        packet.skip(2);
                        planeX = packet.readUnsignedShort();
                        planeY = packet.readUnsignedShort();
                        planeFloor = packet.readUnsignedShort();
                        floorXOffset = packet.readUnsignedShort();
                        break;
                    case PacketBuilder.OPCODE_CREATE_PLAYERS:
                        packet.startBitmask();
                        playerX = packet.readBitmask(11);
                        playerY = packet.readBitmask(13);
                        packet.endBitmask();
                        // fillView(playerX, playerY, objects);
                        packet.skip(packet.data.length - 3);
                        break;
                    case PacketBuilder.OPCODE_UPDATE_PLAYERS: {
                        int originalPlayerCount = packet.readUnsignedShort();
                        int playerCount = originalPlayerCount;
                        for (int i = 0; i < originalPlayerCount; i++) {
                            int startPosition = packet.tell();
                            int pid = packet.readUnsignedShort();
                            int updateType = packet.readUnsignedByte();
                            if (updateType == 0) { // bubble overhead
                                packet.skip(2);
                            } else if (updateType == 1) { // chat
                                packet.skip(1);
                                packet.readRSCString();

                                // Strip Chat
                                if (Settings.sanitizePublicChat) {
                                    int trimSize = packet.tell() - startPosition;
                                    packet.skip(-trimSize);
                                    packet.trim(trimSize);
                                    playerCount--;
                                    continue;
                                }
                            } else if (updateType == 2) { // damage
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
                        if (Settings.sanitizeFriendsIgnore)
                            packet.opcode = ReplayEditor.VIRTUAL_OPCODE_NOP;
                        break;
                    case PacketBuilder.OPCODE_UPDATE_IGNORE:
                        if (Settings.sanitizeFriendsIgnore)
                            packet.opcode = ReplayEditor.VIRTUAL_OPCODE_NOP;
                        break;
                    case PacketBuilder.OPCODE_UPDATE_FRIEND:
                        if (Settings.sanitizeFriendsIgnore)
                            packet.opcode = ReplayEditor.VIRTUAL_OPCODE_NOP;
                        try {
                            packet.readPaddedString(); // Friend's name
                            packet.readPaddedString(); // Friend's old name
                            int onlineStatus = packet.readByte();
                            if (onlineStatus > 1) { // the friend is online, offline can be 0 or 1 see fsnom2@aol.com2/08-03-2018 14.14.44 for 1
                                String world = packet.readPaddedString();
                                if (world.startsWith("Classic")) {
                                    if (onlineStatus == 6) { // same world
                                        int worldNum = Integer.parseInt(world.substring(world.length() - 1));
                                        Scraper.ip_address4 = worldNum;
                                    } else {
                                        int worldNumExcluded = Integer.parseInt(world.substring(world.length() - 1));
                                        if (worldNumExcluded <= 5) {
                                            Scraper.world_num_excluded |= (int)Math.pow(2,worldNumExcluded - 1);
                                        } else {
                                            editor.foundInauthentic = true;
                                            Logger.Warn("Inauthentic amount of worlds");
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Logger.Error(String.format("error parsing opcode_update_friend, packet.timestamp: %d", packet.timestamp));
                        }
                        break;
                    case PacketBuilder.OPCODE_RECV_PM:
                    case PacketBuilder.OPCODE_SEND_PM:
                        if (Settings.sanitizePrivateChat)
                            packet.opcode = ReplayEditor.VIRTUAL_OPCODE_NOP;
                        break;
                    case PacketBuilder.OPCODE_CREATE_NPC:
                        packet.startBitmask();
                        int npcCount = packet.readBitmask(8);

                        // animation updates (ignored for now)
                        int animation;
                        for (int npcIndex = 0; npcIndex < npcCount; npcIndex++) {
                            if (packet.readBitmask(1) == 1) { // updateRequired
                                if (packet.readBitmask(1) != 0) { // updateType
                                    animation = packet.readBitmask(2);
                                    if (animation != 3) {
                                        animation <<= 2;
                                        animation |= packet.readBitmask(2);
                                    }
                                }  else  {
                                    animation = packet.readBitmask(3); // animation
                                }
                            }
                        }
                        while (packet.tellBitmask() + 34 < packet.data.length * 8) {
                            long npcServerIndex = packet.readBitmask(12);
                            int npcXCoordinate = packet.readBitmask(5);
                            int npcYCoordinate = packet.readBitmask(5);
                            int npcAnimation = packet.readBitmask(4);
                            long npcId = packet.readBitmask(10);
                            m_npcLocCSV.put(m_npcLocCSV.size(), String.format("%s,%d,%f,%d,%d,%d,%d\n",
                                    fname,
                                    packet.timestamp,
                                    (packet.timestamp / 50.0) + editor.getReplayMetadata().dateModified,
                                    npcId,
                                    npcServerIndex,
                                    npcXCoordinate + playerX + planeFloor * floorXOffset,
                                    npcYCoordinate + playerY
                            ));
                        }
                        packet.endBitmask();
                        break;
                    case PacketBuilder.OPCODE_CLOSE_CONNECTION_NOTIFY:
                        if (appendingToReplay) {
                            packet.opcode = ReplayEditor.VIRTUAL_OPCODE_NOP;
                        }
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Logger.Error("Scraper.sanitizeReplays incomingPackets loop");
            }
        }

        // Process outgoing packets
        LinkedList<ReplayPacket> outgoingPackets = editor.getOutgoingPackets();
        for (ReplayPacket packet : outgoingPackets) {
            Logger.Debug(String.format("outgoing opcode: %d",packet.opcode));
            try {
                switch (packet.opcode) {
                    case ReplayEditor.VIRTUAL_OPCODE_CONNECT: // Login
                        Logger.Info("outgoing login (timestamp: " + packet.timestamp + ")");
                        break;
                    case 216: // Send chat message
                        if (Settings.sanitizePublicChat)
                            packet.opcode = ReplayEditor.VIRTUAL_OPCODE_NOP;
                        break;
                    case 218: // Send private message
                        if (Settings.sanitizePrivateChat)
                            packet.opcode = ReplayEditor.VIRTUAL_OPCODE_NOP;
                        break;
                    case 167: // Remove friend
                    case 195: // Add friend
                    case 241: // Remove ignore
                    case 132: // Add ignore
                        if (Settings.sanitizeFriendsIgnore)
                            packet.opcode = ReplayEditor.VIRTUAL_OPCODE_NOP;
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Logger.Error("Scraper.sanitizeReplays outgoingPackets loop");
            }
        }

        if (Settings.sanitizeReplays) {
            // Set exported replay version
            if (sanitizeVersion != -1)
                editor.getReplayVersion().version = sanitizeVersion;

            String replayName = fname.substring(Settings.sanitizePath.length());

            String outDir = Settings.sanitizeOutputPath + replayName;
            outDir = new File(outDir).toPath().toAbsolutePath().toString();
            FileUtil.mkdir(outDir);
            editor.exportData(outDir, Settings.sanitizePath);

            // outDir is the folder that everything goes into right now.
            // we would like the base dir, + strippedReplays + pcaps + directory structure + replayName.pcap
            outDir = outDir.replace(Settings.sanitizeBaseOutputPath,Settings.sanitizeBaseOutputPath + "/pcaps");
            FileUtil.mkdir(new File(outDir).getParent());
            editor.exportPCAP(outDir);
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
                File replay = new File(replayDirectory + "/keys.bin");

                if (replay.exists()) {
                    Logger.Info("@|cyan Started sanitizing |@" + replayDirectory);
                    sanitizeReplay(replayDirectory);
                    Logger.Info("@|cyan,intensity_bold Finished sanitizing |@" + replayDirectory);
                } else {
                    sanitizeDirectory(replayDirectory);
                }
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
        System.out.println("\nrscminus v" + Settings.versionNumber+"\n");
        System.out.println("syntax:");
        System.out.println("\t[OPTIONS] [REPLAY DIRECTORY]");
        System.out.println("options:");
        System.out.println("\t-a\t\t\tAppend client test data to the end of all replays being processed");
        System.out.println("\t-d\t\t\tDump objects & other data to binary files");
        System.out.println("\t-f\t\t\tRemove opcodes related to the friend's list");
        System.out.println("\t-h\t\t\tShow this usage dialog");
        System.out.println("\t-p\t\t\tSanitize public chat");
        System.out.println("\t-s\t\t\tExport sanitized replays");
        System.out.println("\t-v<0-" + ReplayEditor.VERSION + ">\t\t\tSet sanitizer replay version (Default is original replay version)");
        System.out.println("\t-x\t\t\tSanitize private chat");
        System.out.println("\t-z\t\t\tProcess replays even if they're not authentic");
    }

    private static boolean parseArguments(String args[]) {
        for (String arg : args) {
            switch(arg.toLowerCase().substring(0, 2)) {
                case "-a":
                    appendingToReplay = true;
                    break;
                case "-d":
                    Settings.dumpObjects = true;
                    Settings.dumpWallObjects = true;
                    Logger.Info("dumping stuff");
                    break;
                case "-f":
                    Settings.sanitizeFriendsIgnore = true;
                    Logger.Info("sanitize Friends Ignore set");
                    break;
                case "-h":
                    return false;
                case "-p":
                    Settings.sanitizePublicChat = true;
                    Logger.Info("sanitize Public Chat set");
                    break;
                case "-s":
                    Settings.sanitizeReplays = true;
                    Logger.Info("sanitize Replays set");
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
                    Settings.sanitizePath = arg;
                    return true;
            }
        }
        return false;
    }
    
    public static void scrape() {
        scraping = true;
        Settings.scraperOutputPath = Settings.Dir.JAR + "/dump/";
        // Scrape directory
        if (Settings.dumpObjects || Settings.dumpWallObjects) {
            FileUtil.mkdir(Settings.scraperOutputPath);
            File replay = new File(Settings.sanitizePath + "/in.bin.gz");
            if (replay.exists())
                scrapeReplay(Settings.sanitizePath);
            else
                scrapeDirectory(Settings.sanitizePath);
        } else {
            Logger.Warn("@|red You attempted to scrape nothing. Make sure to select something to scrape.|@");
        }
        if (Settings.dumpObjects) {
            dumpObjects(Settings.scraperOutputPath + "objects.bin");
        }
        if (Settings.dumpWallObjects) {
            dumpWallObjects(Settings.scraperOutputPath + "wallobjects.bin");
        }
        Logger.Info("Saved to " + Settings.scraperOutputPath);
        Logger.Info("@|green,intensity_bold Finished Scraping!|@");
        scraping = false;
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
        Settings.sanitizeOutputPath = Settings.sanitizeBaseOutputPath;

        m_npcLocCSV = new HashMap<Integer, String>();
        m_npcLocCSV.put(m_npcLocCSV.size(), "replayName,timeStamp,irlTimeStamp,npcId,npcServerIndex (Not Unique!),XCoordinate,YCoordinate");

        FileUtil.mkdir(Settings.sanitizePath);

        Settings.sanitizeOutputPath = Settings.sanitizeOutputPath + "/" + new File(Settings.sanitizePath).getName();
        FileUtil.mkdir(Settings.sanitizeOutputPath);
        Logger.Info("Saving to " + Settings.sanitizeOutputPath);

        File replay = new File(Settings.sanitizePath + "/in.bin.gz");
        if (replay.exists()) {
            sanitizeReplay(Settings.sanitizePath);
        } else {
            sanitizeDirectory(Settings.sanitizePath);
        }

        dumpNPCLocs(Settings.scraperOutputPath + "npcLocs.csv");

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
            // TODO: Combine dumper with sanitizer
            if (Settings.dumpObjects || Settings.dumpWallObjects) {
                scrape();
            }
            if (Settings.sanitizeReplays) {
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
