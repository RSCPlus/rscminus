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
import rscminus.common.Logger;
import rscminus.common.Settings;
import rscminus.game.PacketBuilder;
import rscminus.game.constants.Game;
import rscminus.game.world.ViewRegion;
import rscminus.scraper.client.Character;

import java.io.*;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;

import static rscminus.common.Settings.initDir;
import static rscminus.scraper.ReplayEditor.appendingToReplay;

public class Scraper {
    private static HashMap<Integer, Integer> m_sceneryLocs = new HashMap<Integer, Integer>();
    private static HashMap<Integer, Integer> m_boundaryLocs = new HashMap<Integer, Integer>();
    private static HashMap<Integer, Integer> sceneryLocs = new HashMap<Integer, Integer>();
    private static HashMap<Integer, Integer> boundaryLocs = new HashMap<Integer, Integer>();
    private static HashMap<Integer, String> m_npcLocCSV = new HashMap<Integer, String>();
    private static HashMap<Integer, String> m_replayDictionarySQL = new HashMap<Integer, String>();
    private static HashMap<Integer, String> m_chatSQL = new HashMap<Integer, String>();
    private static HashMap<Integer, String> m_messageSQL = new HashMap<Integer, String>();

    private static final int SCENERY_BLANK = 65536;

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

    public static Character[] npcsCache = new Character[500];
    public static Character[] npcs = new Character[500];
    public static Character[] npcsServer = new Character[5000];
    public static int npcCount = 0;
    public static int npcCacheCount = 0;
    public static int highestOption = 0;

    private static Character createNpc(int serverIndex, int type, int x, int y, int sprite) {
        if (npcsServer[serverIndex] == null) {
            npcsServer[serverIndex] = new Character();
            npcsServer[serverIndex].serverIndex = serverIndex;
        }

        Character character = npcsServer[serverIndex];
        boolean foundNpc = false;

        for (int var9 = 0; npcCacheCount > var9; ++var9) {
            if (serverIndex == npcsCache[var9].serverIndex) {
                foundNpc = true;
                break;
            }
        }

        if (foundNpc) {
            character.animationNext = sprite;
            character.npcId = type;
            int waypointIdx = character.waypointCurrent;
            if (character.waypointsX[waypointIdx] != x || y != character.waypointsY[waypointIdx]) {
                character.waypointCurrent = waypointIdx = (1 + waypointIdx) % 10;
                character.waypointsX[waypointIdx] = x;
                character.waypointsY[waypointIdx] = y;
            }
        } else {
            character.waypointsX[0] = character.currentX = x;
            character.waypointCurrent = 0;
            character.serverIndex = serverIndex;
            character.movingStep = 0;
            character.stepCount = 0;
            character.npcId = type;
            character.waypointsY[0] = character.currentY = y;
            character.animationNext = character.animationCurrent = sprite;
        }

        npcs[npcCount++] = character;
        return character;
    }

    private static boolean sceneryIDBlacklisted(int id, int x, int y) {
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
            Logger.Debug("Scenery id " + id + " at " + x + ", " + y + " was blacklisted");

        return blacklist;
    }

    private static boolean sceneryIDRemoveList(int id, int x, int y) {
        boolean remove = false;

        if (id == 97) // fire
            remove = true;

        if (remove)
            Logger.Debug("Scenery id " + id + " at " + x + ", " + y + " was removed");

        return remove;
    }

    private static boolean boundaryIDBlacklisted(int id, int x, int y) {
        boolean blacklist = false;

        if (blacklist)
            Logger.Debug("Boundary id " + id + " at " + x + ", " + y + " was blacklisted");

        return blacklist;
    }

    private static int handleSceneryIDConflict(int before, int after) {
        if (before == SCENERY_BLANK)
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

        Logger.Warn("unhandled scenery conflict; before: " + before + ", after: " + after);

        return before;
    }

    private static int handleSceneryIDConvert(int id) {
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

    private static int handleBoundaryIDConvert(int value) {
        int id = getPackedX(value);
        int direction = getPackedY(value);

        if (id == 1) // Doorframe
            id = 2;
        else if (id == 9) // Doorframe
            id = 8;

        return packCoordinate(id, direction);
    }

    private static int handleBoundaryIDConflict(int before, int after) {
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

        Logger.Warn("unhandled boundary conflict; before: " + beforeID + ", after: " + afterID);

        return before;
    }

    //convertImage & saveBitmap are mostly courtesy of aposbot, altered a little
    //used for opcode 117, sleepwords
    private static byte[] convertImage(byte[] data) {
        int var1 = 1;
        byte var2 = 0;
        final byte[] var4 = new byte[10200];
        int var3;
        int var5;
        int var6;
        for (var3 = 0; var3 < 255; var2 = (byte) (255 - var2)) {
            var5 = data[var1++] & 255;
            for (var6 = 0; var6 < var5; ++var6) {
                var4[var3++] = var2;
            }
        }
        for (var5 = 1; var5 < 40; ++var5) {
            var6 = 0;
            while (var6 < 255) {
                if (var1++ >= data.length - 1)
                    break;

                final int var7 = data[var1] & 255;
                for (int var8 = 0; var8 < var7; ++var8) {
                    var4[var3] = var4[var3 - 255];
                    ++var3;
                    ++var6;
                }
                if (var6 < 255) {
                    var4[var3] = (byte) (255 - var4[var3 - 255]);
                    ++var3;
                    ++var6;
                }
            }
        }
        return var4;
    }
    private static byte[] saveBitmap(byte[] data) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
        out.write(66);
        out.write(77);
        short var3 = 1342;
        out.write(var3 & 255);
        out.write(var3 >> 8 & 255);
        out.write(0);
        out.write(0);
        out.write(0);
        out.write(0);
        out.write(0);
        out.write(0);
        byte var10 = 62;
        out.write(var10 & 255);
        out.write(var10 >> 8 & 255);
        out.write(0);
        out.write(0);
        var10 = 40;
        out.write(var10 & 255);
        out.write(var10 >> 8 & 255);
        out.write(0);
        out.write(0);
        var3 = 256;
        out.write(var3 & 255);
        out.write(var3 >> 8 & 255);
        out.write(0);
        out.write(0);
        var10 = 40;
        out.write(var10 & 255);
        out.write(var10 >> 8 & 255);
        out.write(0);
        out.write(0);
        var10 = 1;
        out.write(var10 & 255);
        out.write(var10 >> 8 & 255);
        var10 = 1;
        out.write(var10 & 255);
        out.write(var10 >> 8 & 255);
        var10 = 0;
        out.write(var10 & 255);
        out.write(var10 >> 8 & 255);
        out.write(0);
        out.write(0);
        var10 = 0;
        out.write(var10 & 255);
        out.write(var10 >> 8 & 255);
        out.write(0);
        out.write(0);
        var10 = 0;
        out.write(var10 & 255);
        out.write(var10 >> 8 & 255);
        out.write(0);
        out.write(0);
        var10 = 0;
        out.write(var10 & 255);
        out.write(var10 >> 8 & 255);
        out.write(0);
        out.write(0);
        var10 = 0;
        out.write(var10 & 255);
        out.write(var10 >> 8 & 255);
        out.write(0);
        out.write(0);
        var10 = 0;
        out.write(var10 & 255);
        out.write(var10 >> 8 & 255);
        out.write(0);
        out.write(0);
        out.write(0);
        out.write(0);
        out.write(0);
        out.write(0);
        out.write(255);
        out.write(255);
        out.write(255);
        out.write(0);
        int var4 = 9945;
        for (int var5 = 0; var5 < 40; ++var5) {
            for (int var6 = 0; var6 < 32; ++var6) {
                byte var7 = 0;
                for (int var8 = 0; var8 < 8; ++var8) {
                    var7 = (byte) (2 * var7);
                    if (var6 != 31 || var8 != 7) {
                        if (data[var4] != 0) {
                            ++var7;
                        }
                        ++var4;
                    }
                }
                out.write(var7);
            }
            var4 -= 510;
        }
        out.close();
        return out.toByteArray();
    }

    private static void dumpScenery(String fname) {
        int sceneryCount = 0;
        for (HashMap.Entry<Integer, Integer> entry : m_sceneryLocs.entrySet()) {
            if (entry.getValue() != SCENERY_BLANK)
                sceneryCount++;
        }
        try {
            DataOutputStream out = new DataOutputStream(new FileOutputStream(new File(fname)));
            out.writeInt(sceneryCount);
            for (HashMap.Entry<Integer, Integer> entry : m_sceneryLocs.entrySet()) {
                int x = getPackedX(entry.getKey());
                int y = getPackedY(entry.getKey());
                int id = entry.getValue();
                if (id != SCENERY_BLANK) {
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
            Logger.Info("Dumped " + count + " Boundary locations");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void dumpNPCLocs(String fname) {
        try {
            Logger.Info("There's  a whopping " +  (m_npcLocCSV.size() - 1) + " NPC locations to dump. Please hold.");
            DataOutputStream out = new DataOutputStream(new FileOutputStream(new File(fname)));
            for (HashMap.Entry<Integer, String> entry : m_npcLocCSV.entrySet()) {
                out.writeBytes(entry.getValue());
            }
            out.close();
            Logger.Info("Dumped " + (m_npcLocCSV.size() - 1) + " NPC locations");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void dumpSQLToFile(HashMap<Integer, String> sqlStatements, String identifier, String fname) {
        try {
            int size = (sqlStatements.size() - 1);
            Logger.Info(String.format("@|green There's a whopping %d %s%s to dump. Please hold.|@", size, identifier, size == 1 ? "" : "s"));
            DataOutputStream out = new DataOutputStream(new FileOutputStream(new File(fname)));
            for (HashMap.Entry<Integer, String> entry : sqlStatements.entrySet()) {
                out.writeBytes(entry.getValue());
            }
            out.close();
            Logger.Info(String.format("@|green,intensity_bold Dumped %d %s sql statement%s!|@", size, identifier, size == 1 ? "" : "s"));
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

    private static void fillView(int playerX, int playerY, HashMap<Integer, Integer> scenery) {
        int viewX = (playerX >> 3) << 3;
        int viewY = (playerY >> 3) << 3;
        int size = ViewRegion.VIEW_DISTANCE << 3;
        int index = (ViewRegion.VIEW_DISTANCE / 2) << 3;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                int updateX = viewX + (x - index);
                int updateY = viewY + (y - index);
                int key = packCoordinate(updateX, updateY);
                if (!scenery.containsKey(key))
                    scenery.put(key, SCENERY_BLANK);
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

    private static void processReplay(String fname) {
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

        int localPID = -1;
        int playerX = -1;
        int playerY = -1;
        int planeX = -1;
        int planeY = -1;
        int planeFloor = -1;
        int floorYOffset = -1;
        int length = -1;

        if (Settings.dumpMessages || Settings.dumpChat) {
            m_replayDictionarySQL.put(m_replayDictionarySQL.size(),
                    String.format("%s%d', '%s');\n",
                            "INSERT INTO `rscMessages`.`replayDictionary` (`index`, `filePath`) VALUES ('",
                            Scraper.replaysProcessedCount,
                            fname.replaceFirst(Settings.sanitizePath, "").replaceAll("'", "''")
                    )
            );
        }

        int op234type1EchoCount = 0;
        int sendPMEchoCount = 0;

        HashMap<Integer, ReplayPacket> interestingSleepPackets = new HashMap<Integer, ReplayPacket>();

        // Process incoming packet
        LinkedList<ReplayPacket> incomingPackets = editor.getIncomingPackets();
        for (ReplayPacket packet : incomingPackets) {
            Logger.Debug(String.format("incoming opcode: %d",packet.opcode));
            try {

                switch (packet.opcode) {
                    case ReplayEditor.VIRTUAL_OPCODE_CONNECT:
                        Logger.Info("loginresponse: " + packet.data[0] + " (timestamp: " + packet.timestamp + ")");

                        if (Settings.dumpSleepWords) {
                            interestingSleepPackets.put(interestingSleepPackets.size(), packet);
                        }

                        break;
                    case PacketBuilder.OPCODE_FLOOR_SET:
                        localPID = packet.readUnsignedShort();
                        planeX = packet.readUnsignedShort();
                        planeY = packet.readUnsignedShort();
                        planeFloor = packet.readUnsignedShort();
                        floorYOffset = packet.readUnsignedShort();
                        break;
                    case PacketBuilder.OPCODE_SEND_MESSAGE:
                        if (Settings.dumpMessages || Settings.dumpSleepWords) {
                            int type = packet.readUnsignedByte();
                            int infoContained = packet.readUnsignedByte();

                            String sendMessage = packet.readPaddedString();

                            if (Settings.dumpSleepWords) {
                                if (sendMessage.equals("You are unexpectedly awoken! You still feel tired")) {
                                    interestingSleepPackets.put(interestingSleepPackets.size(), packet);
                                }
                            }

                            if (Settings.dumpMessages) {
                                String sender = "";
                                String clan = "";
                                String color = "";
                                if ((infoContained & 1) != 0) {
                                    sender = packet.readPaddedString();
                                    clan = packet.readPaddedString();
                                }

                                if ((infoContained & 2) != 0) {
                                    color = packet.readPaddedString();
                                }
                                m_messageSQL.put(m_messageSQL.size(),
                                    "INSERT INTO `rscMessages`.`SEND_MESSAGE` (`replayIndex`, `timestamp`, `messageType`, `infoContained`, `message`, `sender`, `sender2`, `color`) VALUES " +
                                        String.format("('%d', '%d', '%d', '%d', '%s', '%s', '%s', '%s');\n",
                                            Scraper.replaysProcessedCount,
                                            packet.timestamp,
                                            type,
                                            infoContained,
                                            sendMessage.replaceAll("'", "''"),
                                            sender,
                                            clan,
                                            color)
                                );
                            }
                        }
                        break;
                    case PacketBuilder.OPCODE_DIALOGUE_OPTIONS:
                        if (Settings.dumpMessages) {
                            int numberOfOptions = packet.readUnsignedByte();
                            if (numberOfOptions > highestOption) {
                                highestOption = numberOfOptions;
                            }
                            String dialogueOptionsInsert = "INSERT INTO `rscMessages`.`DIALOGUE_OPTION` (`replayIndex`, `timestamp`, `numberOfOptions`";
                            String dialogueOptionsValues = String.format("'%d', '%d', '%d'", Scraper.replaysProcessedCount, packet.timestamp, numberOfOptions);

                            for (int i = 1; i <= numberOfOptions; i++) {
                                dialogueOptionsInsert = String.format("%s%s%d%s",dialogueOptionsInsert, ", `choice", i, "`");
                                dialogueOptionsValues += String.format(", '%s'", packet.readPaddedString().replaceAll("'", "''"));
                            }

                            m_messageSQL.put(m_messageSQL.size(), String.format("%s%s%s%s",
                                    dialogueOptionsInsert,
                                    ") VALUES (",
                                    dialogueOptionsValues,
                                    ");\n")
                            );
                        }
                        break;
                    case PacketBuilder.OPCODE_CREATE_PLAYERS:
                        packet.startBitmask();
                        playerX = packet.readBitmask(11);
                        playerY = packet.readBitmask(13);
                        packet.endBitmask();
                        if (Settings.dumpScenery) {
                            fillView(playerX, playerY, sceneryLocs);
                        }
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
                                String chatMessage = packet.readRSCString();
                                if (Settings.dumpChat) {
                                    m_chatSQL.put(m_chatSQL.size(),
                                      "INSERT INTO `rscMessages`.`UPDATE_PLAYERS_TYPE_1` (`replayIndex`, `timestamp`, `pid`, `localPlayerMessageCount`, `message`) VALUES " +
                                          String.format("('%d', '%d', '%d', '%d', '%s');\n",
                                              Scraper.replaysProcessedCount,
                                              packet.timestamp,
                                              pid,
                                              pid == localPID ? op234type1EchoCount++ : -1,
                                              chatMessage.replaceAll("'", "''")
                                          )
                                    );
                                }

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
                            } else if (updateType == 6) { // quest chat
                                String message = packet.readRSCString();
                                if (Settings.dumpMessages) {
                                    m_messageSQL.put(m_messageSQL.size(),
                                            "INSERT INTO `rscMessages`.`UPDATE_PLAYERS_TYPE_6` (`replayIndex`, `timestamp`, `pid`, `message`) VALUES " +
                                                    String.format("('%d', '%d', '%d', '%s');\n",
                                                            Scraper.replaysProcessedCount,
                                                            packet.timestamp,
                                                            pid,
                                                            message.replaceAll("'", "''")
                                                    )
                                    );
                                }
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
                        if (Settings.dumpChat) {
                            String recvPMSender1 = packet.readPaddedString(); //sender's name
                            String recvPMSender2 = packet.readPaddedString(); //sender's name again
                            Boolean recvPMSenderTwice = recvPMSender1.equals(recvPMSender2);
                            if (!recvPMSenderTwice) {
                                Logger.Warn("Sender1 != Sender2 in OPCODE_RECV_PM!");
                            }
                            int recvPMModeratorStatus = packet.readByte();
                            BigInteger recvPMMessageID = packet.readUnsignedLong();
                            String recvPMMessage = packet.readRSCString();

                            m_chatSQL.put(m_chatSQL.size(),
                                "INSERT INTO `rscMessages`.`RECEIVE_PM` (`replayIndex`, `timestamp`, `sendersRepeated`, `moderator`, `messageID`, `message`) VALUES " +
                                    String.format("('%d', '%d', '%s', '%d', '%d', '%s');\n",
                                        Scraper.replaysProcessedCount,
                                        packet.timestamp,
                                        recvPMSenderTwice ? "1" : "0",
                                        recvPMModeratorStatus,
                                        recvPMMessageID,
                                        recvPMMessage.replaceAll("'", "''")
                                    )
                            );
                        }

                        if (Settings.sanitizePrivateChat)
                            packet.opcode = ReplayEditor.VIRTUAL_OPCODE_NOP;
                        break;
                    case PacketBuilder.OPCODE_SEND_PM:
                        if (Settings.dumpChat) {
                            packet.readPaddedString(); //recipient's name
                            String sendPMMessage = packet.readRSCString();
                            m_chatSQL.put(m_chatSQL.size(),
                                "INSERT INTO `rscMessages`.`SEND_PM_SERVER_ECHO` (`replayIndex`, `timestamp`, `messageCount`, `message`) VALUES " +
                                    String.format("('%d', '%d', '%d', '%s');\n",
                                        Scraper.replaysProcessedCount,
                                        packet.timestamp,
                                        sendPMEchoCount++,
                                        sendPMMessage.replaceAll("'", "''")
                                    )
                            );
                        }
                        if (Settings.sanitizePrivateChat)
                            packet.opcode = ReplayEditor.VIRTUAL_OPCODE_NOP;
                        break;
                    case PacketBuilder.OPCODE_CREATE_NPC:
                        if (Settings.needNpcCreation) {
                            npcCacheCount = npcCount;
                            npcCount = 0;
                            for (int index = 0; index < npcCacheCount; ++index) {
                                npcsCache[index] = npcs[index];
                            }

                            packet.startBitmask();
                            int createNpcCount = packet.readBitmask(8);
                            int animation;
                            for (int npcIndex = 0; npcIndex < createNpcCount; npcIndex++) {
                                Character npc = npcsCache[npcIndex];
                                int reqUpdate = packet.readBitmask(1);
                                if (reqUpdate == 1) {
                                    int updateType = packet.readBitmask(1);
                                    if (updateType != 0) { // stationary animation update
                                        animation = packet.readBitmask(2);
                                        if (animation != 3) {
                                            animation <<= 2;
                                            animation |= packet.readBitmask(2);

                                            npc.animationNext = animation;
                                        } else {
                                            // npc is removed
                                            continue;
                                        }
                                    } else { // npc is moving to another tile
                                        int nextAnim = packet.readBitmask(3); // animation
                                        int var11 = npc.waypointCurrent;
                                        int var12 = npc.waypointsX[var11];
                                        if (nextAnim == 2 || nextAnim == 1 || nextAnim == 3) {
                                            var12 += 128;
                                        }

                                        int var13 = npc.waypointsY[var11];
                                        if (nextAnim == 6 || nextAnim == 5 || nextAnim == 7) {
                                            var12 -= 128;
                                        }

                                        if (nextAnim == 4 || nextAnim == 3 || nextAnim == 5) {
                                            var13 += 128;
                                        }

                                        if (nextAnim == 0 || nextAnim == 1 || nextAnim == 7) {
                                            var13 -= 128;
                                        }

                                        npc.waypointCurrent = var11 = (var11 + 1) % 10;
                                        npc.animationNext = nextAnim;
                                        npc.waypointsX[var11] = var12;
                                        npc.waypointsY[var11] = var13;
                                    }
                                }

                                npcs[npcCount++] = npc;
                            }


                            while (packet.tellBitmask() + 34 < packet.data.length * 8) {
                                int npcServerIndex = packet.readBitmask(12);
                                int npcXCoordinate = packet.readBitmask(5);
                                int npcYCoordinate = packet.readBitmask(5);
                                int npcAnimation = packet.readBitmask(4);
                                int npcId = packet.readBitmask(10);

                                if (npcXCoordinate > 15) {
                                    npcXCoordinate -= 32;
                                }
                                if (npcYCoordinate > 15) {
                                    npcYCoordinate -= 32;
                                }

                                if (Settings.dumpNpcLocs) {
                                    m_npcLocCSV.put(m_npcLocCSV.size(), String.format("%s,%d,%f,%d,%d,%d,%d\n",
                                            fname,
                                            packet.timestamp,
                                            (packet.timestamp / 50.0) + editor.getReplayMetadata().dateModified,
                                            npcId,
                                            npcServerIndex,
                                            npcXCoordinate + playerX + planeFloor * floorYOffset,
                                            npcYCoordinate + playerY
                                    ));
                                }
                                int x = npcXCoordinate + playerX + planeFloor * floorYOffset;
                                int y = npcYCoordinate + playerY;

                                createNpc(npcServerIndex, npcId, x, y, npcAnimation);
                            }

                            packet.endBitmask();
                        }

                        break;
                    case PacketBuilder.OPCODE_UPDATE_NPC:
                        if (Settings.dumpMessages) {
                            int updateNpcCount = packet.readUnsignedShort();
                            for (int i = 0; i < updateNpcCount; i++) {
                                int npcServerIndex = packet.readUnsignedShort();
                                int updateType = packet.readByte();
                                if (updateType == 1) { // npc chat
                                    int pidTalkingTo = packet.readUnsignedShort();
                                    String updateNPCMessage = packet.readRSCString();
                                    m_messageSQL.put(m_messageSQL.size(),
                                            "INSERT INTO `rscMessages`.`UPDATE_NPCS_TYPE_1` (`replayIndex`, `timestamp`, `npcId`, `pidTalkingTo`, `message`) VALUES " +
                                                    String.format("('%d', '%d', '%d', '%d', '%s');\n",
                                                            Scraper.replaysProcessedCount,
                                                            packet.timestamp,
                                                            npcsServer[npcServerIndex].npcId,
                                                            pidTalkingTo,
                                                            updateNPCMessage.replaceAll("'", "''")
                                                    )
                                    );
                                }
                            }
                        }
                        break;
                    case PacketBuilder.OPCODE_SCENERY_HANDLER:
                        if (Settings.dumpScenery) {
                            length = packet.data.length;
                            while (length > 0) {
                                if (packet.readUnsignedByte() == 255) {
                                    packet.skip(2);
                                    length -= 3;
                                } else {
                                    packet.skip(-1);
                                    int type = handleSceneryIDConvert(packet.readUnsignedShort());
                                    int x = playerX + packet.readByte();
                                    int y = playerY + packet.readByte();
                                    length -= 4;

                                    if (planeX != Game.WORLD_PLANE_X || planeY != Game.WORLD_PLANE_Y || floorYOffset != Game.WORLD_Y_OFFSET || planeFloor > 3 || planeFloor < 0) {
                                        Logger.Error("Invalid region or not logged in; Aborting");
                                        break;
                                    }

                                    if (!validCoordinates(x, y)) {
                                        Logger.Error("Invalid coordinates " + x + ", " + y + "; Aborting");
                                        break;
                                    } else if (type != 60000 && !sceneryIDBlacklisted(type, x, y)) {
                                        if (type < 0 || type > 1188) {
                                            Logger.Error("Scenery id " + type + " at " + x + ", " + y + " is invalid; Aborting");
                                            break;
                                        }

                                        int key = packCoordinate(x, y);
                                        //System.out.println("x: " + x + ", y: " + y);
                                        if (sceneryLocs.containsKey(key))
                                            type = handleSceneryIDConflict(sceneryLocs.get(key), type);
                                        sceneryLocs.put(key, type);
                                    }
                                }
                            }
                        }
                        break;
                    case PacketBuilder.OPCODE_BOUNDARY_HANDLER:
                        if (Settings.dumpBoundaries) {
                            length = packet.data.length;
                            while (length > 0) {
                                if (packet.readUnsignedByte() == 255) {
                                    packet.skip(2);
                                    length -= 3;
                                } else {
                                    packet.skip(-1);
                                    int type = packet.readUnsignedShort();
                                    int x = playerX + packet.readByte();
                                    int y = playerY + packet.readByte();
                                    byte direction = packet.readByte();
                                    length -= 5;

                                    if (planeX != Game.WORLD_PLANE_X || planeY != Game.WORLD_PLANE_Y || floorYOffset != Game.WORLD_Y_OFFSET || planeFloor > 3 || planeFloor < 0) {
                                        Logger.Error("Invalid region or not logged in; Aborting");
                                        break;
                                    }

                                    if (!validCoordinates(x, y)) {
                                        Logger.Error("Invalid coordinates " + x + ", " + y + "; Aborting");
                                        break;
                                    } else if (type != 0xFFFF && !boundaryIDBlacklisted(type, x, y)) {
                                        if (type < 0 || type > 213) {
                                            Logger.Error("Boundary id " + type + " at " + x + ", " + y + " is invalid; Aborting");
                                            break;
                                        }

                                        int key = packCoordinate(x, y);
                                        int value = handleBoundaryIDConvert(packCoordinate(type, direction));
                                        if (boundaryLocs.containsKey(key))
                                            value = handleBoundaryIDConflict(boundaryLocs.get(key), value);
                                        boundaryLocs.put(key, value);
                                    }
                                }
                            }
                        }
                        break;
                    case PacketBuilder.OPCODE_SLEEP_WORD:
                        if (Settings.dumpSleepWords) {
                            interestingSleepPackets.put(interestingSleepPackets.size(), packet);
                        }
                        break;
                    case PacketBuilder.OPCODE_WAKE_UP:
                        if (Settings.dumpSleepWords) {
                            interestingSleepPackets.put(interestingSleepPackets.size(), packet);
                        }
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
                Logger.Error(String.format("@|red Scraper.processReplays incomingPackets loop during replay %s on opcode %d at timestamp %d|@", fname, packet.opcode, packet.timestamp));
            }
        }

        if (Settings.dumpScenery) {
            for (HashMap.Entry<Integer, Integer> entry : sceneryLocs.entrySet()) {
                int key = entry.getKey();
                int id = entry.getValue();
                if (m_sceneryLocs.containsKey(key)) {
                    int oldID = m_sceneryLocs.get(key);
                    if (oldID == SCENERY_BLANK)
                        continue;
                    if (id == SCENERY_BLANK && oldID != SCENERY_BLANK && sceneryIDRemoveList(oldID, getPackedX(key), getPackedY(key))) {
                        m_sceneryLocs.put(key, id);
                        continue;
                    }
                    if (id != SCENERY_BLANK) {
                        id = handleSceneryIDConflict(m_sceneryLocs.get(key), id);
                        m_sceneryLocs.put(key, id);
                    }
                } else {
                    m_sceneryLocs.put(key, id);
                }
            }
        }
        if (Settings.dumpBoundaries) {
            for (HashMap.Entry<Integer, Integer> entry : boundaryLocs.entrySet()) {
                int key = entry.getKey();
                int value = entry.getValue();
                if (m_boundaryLocs.containsKey(key))
                    value = handleBoundaryIDConflict(m_boundaryLocs.get(key), value);
                m_boundaryLocs.put(key, value);
            }
        }

        int op216Count = 0;
        int sendPMCount = 0;

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
                        if (Settings.dumpChat) {
                            String sendChatMessage = packet.readRSCString();
                            m_chatSQL.put(m_chatSQL.size(),
                                "INSERT INTO `rscMessages`.`SEND_CHAT` (`replayIndex`, `timestamp`, `pid`, `sendCount`, `message`) VALUES " +
                                    String.format("('%d', '%d', '%d', '%d', '%s');\n",
                                        Scraper.replaysProcessedCount,
                                        packet.timestamp,
                                        localPID,
                                        op216Count++,
                                        sendChatMessage.replaceAll("'", "''")
                                    )
                            );
                        }
                        if (Settings.sanitizePublicChat)
                            packet.opcode = ReplayEditor.VIRTUAL_OPCODE_NOP;
                        break;

                    case 218: // Send private message
                        if (Settings.dumpChat) {
                            packet.readPaddedString(); // recipient
                            String sendPMMessage = packet.readRSCString();
                            m_chatSQL.put(m_chatSQL.size(),
                                "INSERT INTO `rscMessages`.`SEND_PM` (`replayIndex`, `timestamp`, `messageCount`, `message`) VALUES " +
                                    String.format("('%d', '%d', '%d', '%s');\n",
                                        Scraper.replaysProcessedCount,
                                        packet.timestamp,
                                        sendPMCount++,
                                        sendPMMessage.replaceAll("'", "''")
                                    )
                            );
                        }
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

                    case 116: // Choose dialogue option
                        if (Settings.dumpMessages) {
                            m_messageSQL.put(m_messageSQL.size(),
                                    String.format("INSERT INTO `rscMessages`.`CLIENT_CHOOSE_DIALOGUE_OPTION` (`replayIndex`, `timestamp`, `choice`) VALUES ('%d', '%d', '%d');\n",
                                            Scraper.replaysProcessedCount,
                                            packet.timestamp,
                                            packet.readUnsignedByte() + 1
                                    )
                            );
                        }
                        break;
                    case 45: // Send Sleepword Guess
                        if (Settings.dumpSleepWords) {
                            interestingSleepPackets.put(interestingSleepPackets.size(), packet);
                        }
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Logger.Error(String.format("@|red Scraper.processReplays outgoingPackets loop during replay %s on opcode %d at timestamp %d|@", fname, packet.opcode, packet.timestamp));
            }
        }

        // go through both incoming & outgoing packets in chronological order, for ease of logic
        if (Settings.dumpSleepWords) {

            int numberOfPackets = interestingSleepPackets.size();
            ReplayPacket[] sleepPackets = new ReplayPacket[numberOfPackets];

            // Populate sleepPackets with sorted packets
            List<ReplayPacket> sortedSleepPackets = new ArrayList<ReplayPacket>(interestingSleepPackets.values());
            try {
                Collections.sort(sortedSleepPackets, new ReplayPacketComparator());
            } catch (Exception e) {
                e.printStackTrace();
            }
            int arrIndex = -1;
            for (Iterator<ReplayPacket> iterator = sortedSleepPackets.iterator(); iterator.hasNext();) {
                sleepPackets[++arrIndex] = iterator.next();
            }

            for (int cur = 0; cur < numberOfPackets; cur++) {
                Logger.Info(String.format("Timestamp: %d; Opcode: %d;",
                    sleepPackets[cur].timestamp,
                    sleepPackets[cur].opcode));
                ReplayPacket packet = sleepPackets[cur];

                switch (sleepPackets[cur].opcode) {
                    case PacketBuilder.OPCODE_SLEEP_WORD:
                        String sleepWordGuess = "";
                        boolean guessCorrect = false;
                        boolean loggedOut = false;
                        boolean awokeEarly = false;

                        if (packet.data.length > 0) {

                            // Find event that ends this sleep session.
                            // This is the reason for putting everything interesting
                            // into an array in which we can freely hop between packets btw...
                            int sleepSessionEnd = -1;
                            for (int i = cur + 1; i < numberOfPackets && sleepSessionEnd == -1; ++i) {
                                switch(sleepPackets[i].opcode) {
                                    case ReplayEditor.VIRTUAL_OPCODE_CONNECT:
                                        loggedOut = true; // user disconnected while asleep & reconnects in awake state
                                        break;
                                    case 45: // CLIENT_SEND_SLEEPWORD_GUESS
                                        sleepPackets[i].readByte();
                                        sleepWordGuess = sleepPackets[i].readPaddedString();
                                        Logger.Info("Found guess: " + sleepWordGuess);
                                        guessCorrect = true; // not known yet, just setting flag true so it can be set false later
                                        break;
                                    case PacketBuilder.OPCODE_SEND_MESSAGE:
                                        awokeEarly = true;
                                        break;
                                    case PacketBuilder.OPCODE_WAKE_UP:
                                        // unexpectedly woke up, even if there is a good guess
                                        if (i + 1 < numberOfPackets) {
                                            if (sleepPackets[i + 1].opcode == PacketBuilder.OPCODE_SEND_MESSAGE) {
                                                awokeEarly = true;
                                            }
                                        }
                                        sleepSessionEnd = cur;
                                        break;
                                    case PacketBuilder.OPCODE_SLEEP_WORD:
                                        guessCorrect = false;
                                        sleepSessionEnd = cur;
                                        break;
                                }
                            }

                            String sleepWordFilePath = String.format("%ssleepwords", Settings.scraperOutputPath);
                            String sleepWordFileName = "";

                            if (guessCorrect && !awokeEarly && !loggedOut) {
                                sleepWordFileName = String.format("sleep_%s%s_%d.",
                                    sleepWordGuess,
                                    fname.replaceFirst(Settings.sanitizePath, "").replaceAll("/", "_"),
                                    cur);
                            } else if (awokeEarly) {
                                sleepWordFileName = String.format("sleep_!SUDDENLY-AWOKE!%s_%d.",
                                    fname.replaceFirst(Settings.sanitizePath, "").replaceAll("/", "_"),
                                    cur);
                            } else if (!guessCorrect) {
                                sleepWordFileName = String.format("sleep_!INCORRECT!%s_%s_%d.",
                                    sleepWordGuess,
                                    fname.replaceFirst(Settings.sanitizePath, "").replaceAll("/", "_"),
                                    cur);
                            } else {
                                sleepWordFileName = String.format("sleep_!LOGGED-OUT!%s_%d.",
                                    fname.replaceFirst(Settings.sanitizePath, "").replaceAll("/", "_"),
                                    cur);
                            }

                            byte[] data = new byte[packet.data.length];

                            for (int i = 0; i < packet.data.length; i++) {
                                data[i] = packet.data[i];
                                System.out.print(String.format("%x", data[i]));
                            }
                            System.out.println();

                            // convert to BMP (mostly for fun)
                            try {
                                data = convertImage(data);
                                File fileName = new File(new File(sleepWordFilePath, "/images/"), sleepWordFileName + "bmp");
                                try (FileOutputStream fos = new FileOutputStream(fileName)) {
                                    fos.write(saveBitmap(data));
                                }
                            } catch (Exception e) {
                                //never happens btw
                                e.printStackTrace();
                            }

                            // export raw packet data
                            try {
                                File fileName = new File(new File(sleepWordFilePath, "/packetData/"), sleepWordFileName + "bin");
                                try (FileOutputStream fos = new FileOutputStream(fileName)) {
                                    fos.write(packet.data);
                                }
                            } catch (Exception e) {
                                //never happens btw
                                e.printStackTrace();
                            }

                            Logger.Info(String.format("sleepword %d: %d length: %d", cur, packet.opcode, packet.data.length));

                        } else {
                            Logger.Warn("Zero length packet 117 in " + fname);
                        }
                        break;
                }

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
            if (false) {
                editor.justUpdateMetadata(outDir, Settings.sanitizePath);
            } else {
                editor.exportData(outDir, Settings.sanitizePath);
            }

            // outDir is the folder that everything goes into right now.
            // we would like the base dir, + strippedReplays + pcaps + directory structure + replayName.pcap
            outDir = outDir.replace(Settings.sanitizeBaseOutputPath,Settings.sanitizeBaseOutputPath + "/pcaps");
            FileUtil.mkdir(new File(outDir).getParent());
            editor.exportPCAP(outDir);
        }
        Scraper.replaysProcessedCount += 1;
    }

    private static void processDirectory(String path) {
        File[] files = new File(path).listFiles();
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f.isDirectory()) {
                String replayDirectory = f.getAbsolutePath();
                File replay = new File(replayDirectory + "/keys.bin");

                if (replay.exists()) {
                    Logger.Info("@|cyan Started sanitizing |@" + replayDirectory);
                    processReplay(replayDirectory);
                    Logger.Info("@|cyan,intensity_bold Finished sanitizing |@" + replayDirectory);
                } else {
                    processDirectory(replayDirectory);
                }
            }
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
                    Settings.sanitizePath = arg;
                    return true;
            }
        }
        return false;
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
            m_npcLocCSV = new HashMap<Integer, String>();
            m_npcLocCSV.put(m_npcLocCSV.size(), "replayName,timeStamp,irlTimeStamp,npcId,npcServerIndex (Not Unique!),XCoordinate,YCoordinate");
        }

        FileUtil.mkdir(Settings.sanitizePath);

        Settings.sanitizeOutputPath = Settings.sanitizeOutputPath + "/" + new File(Settings.sanitizePath).getName();
        FileUtil.mkdir(Settings.sanitizeOutputPath);
        Logger.Info("Saving to " + Settings.sanitizeOutputPath);

        File replay = new File(Settings.sanitizePath + "/in.bin.gz");
        if (replay.exists()) {
            processReplay(Settings.sanitizePath);
        } else {
            processDirectory(Settings.sanitizePath);
        }
        if (Settings.dumpMessages) {
            Logger.Info(String.format("Highest option was: %d", highestOption));
            m_messageSQL.put(m_messageSQL.size(), String.format("/* ----- Highest option was: %d ----- */", highestOption));
            dumpSQLToFile(m_messageSQL, "message", Settings.scraperOutputPath + "allMessages.sql");
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
