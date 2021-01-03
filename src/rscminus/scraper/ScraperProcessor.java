package rscminus.scraper;

import rscminus.common.FileUtil;
import rscminus.common.JGameData;
import rscminus.common.Logger;
import rscminus.common.Settings;
import rscminus.game.PacketBuilder;
import rscminus.game.constants.Game;
import rscminus.game.world.ViewRegion;
import rscminus.scraper.client.Character;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

import static rscminus.scraper.ReplayEditor.appendingToReplay;

public class ScraperProcessor implements Runnable {

    private static HashMap<Integer, Integer> sceneryLocs = new HashMap<Integer, Integer>();
    private static HashMap<Integer, Integer> boundaryLocs = new HashMap<Integer, Integer>();

    public Character[] npcsCache = new Character[500];
    public Character[] npcs = new Character[500];
    public Character[] npcsServer = new Character[5000];
    public int npcCount = 0;
    public int npcCacheCount = 0;
    public int highestOption = 0;
    public int highestStoreStock = 0;

    public int[] inventoryItems = new int[30];
    public long[] inventoryStackAmounts = new long[30];
    public int[] inventoryItemEquipped = new int[30];
    public int[] inventoryItemsAllCount = new int[1290];
    public int lastAmmo = -1;

    public int[] playerBaseStat = new int[18];
    public int[] playerCurStat = new int[18];
    public long[] playerXP = new long[18];

    public String lastSound = "";

    public static final int SCENERY_BLANK = 65536;

    String fname;

    @Override
    public void run() {
        processReplay(fname);
    }

    public ScraperProcessor(String filename) {
        this.fname = filename;
    }

    private Character createNpc(int serverIndex, int type, int x, int y, int sprite) {
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
        int dataIndex = 1;
        byte color = 0;
        final byte[] imageBytes = new byte[10200];
        int index;
        int height;
        int width;
        for (index = 0; index < 255; color = (byte) (255 - color)) {
            height = data[dataIndex++] & 255;
            for (width = 0; width < height; ++width) {
                imageBytes[index++] = color;
            }
        }
        for (height = 1; height < 40; ++height) {
            width = 0;
            while (width < 255) {
                if (dataIndex++ >= data.length - 1)
                    break;

                // run length encoded
                final int rle = data[dataIndex] & 255;
                for (int i = 0; i < rle; ++i) {
                    imageBytes[index] = imageBytes[index - 255];
                    ++index;
                    ++width;
                }
                if (width < 255) {
                    imageBytes[index] = (byte) (255 - imageBytes[index - 255]);
                    ++index;
                    ++width;
                }
            }
        }
        return imageBytes;
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

    private static int packCoordinate(int x, int y) {
        return ((x & 0xFFFF) << 16) | (y & 0xFFFF);
    }

    public static int getPackedX(int value) {
        return (value >> 16) & 0xFFFF;
    }

    public static int getPackedY(int value) {
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

    private static boolean isAmmo(int itemID) {
        switch (itemID) {
            case 11:
            case 190:
            case 574:
            case 592:
            case 638:
            case 639:
            case 640:
            case 641:
            case 642:
            case 643:
            case 644:
            case 645:
            case 646:
            case 647:
            case 723:
            case 786:
            case 984:
            case 985:
            case 827:

            case 1013:
            case 1014:
            case 1015:
            case 1024:
            case 1068:
            case 1069:
            case 1070:

            case 1075:
            case 1076:
            case 1077:
            case 1078:
            case 1079:
            case 1080:
            case 1081:

            case 1088:
            case 1089:
            case 1090:
            case 1091:
            case 1092:
            case 1122:
            case 1123:
            case 1124:
            case 1125:
            case 1126:
            case 1127:
            case 1128:
            case 1129:
            case 1130:
            case 1131:
            case 1132:
            case 1133:
            case 1134:
            case 1135:
            case 1136:
            case 1137:
            case 1138:
            case 1139:
            case 1140:
                return true;
            default:
                return false;
        }
    }

    private boolean removalIsKillshot(Character npc, int localPID, int playerX, int playerY) {
        // npc actively has an unprocessed hit against them, and it is by the local player.
        if (npc.attackingPlayerServerIndex == localPID) {
            return true;
        }

        // Player is close enough to the NPC when it despawns that it is not removed due to being too far away
        if (Math.abs(npc.currentX - playerX) < 12 &&
            Math.abs(npc.currentY - playerY) < 12) {
            // Player can't possibly be in melee combat, because they are not on the same tile
            if (Math.abs(npc.currentX - playerX) > 0 ||
                Math.abs(npc.currentY - playerY) > 0 ) {
                // Player previously was in ranged combat with this NPC
                if (npc.lastAttackerIndex != -1 && npc.lastAttackerIndex == localPID) {
                    // Player's last major action involved defeating an enemy
                    if (lastSound.equals("victory")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // this sets the metadata byte in the replayeditor for when it gets written out to file
    private void setFlags(ReplayEditor editor) {
        byte[] metadata = editor.getMetadata();
        if (Settings.sanitizePublicChat)
            metadata[ReplayEditor.METADATA_FLAGS_OFFSET] |= ReplayEditor.FLAG_SANITIZE_PUBLIC;
        if (Settings.sanitizePrivateChat)
            metadata[ReplayEditor.METADATA_FLAGS_OFFSET] |= ReplayEditor.FLAG_SANITIZE_PRIVATE;
        if (Settings.sanitizeFriendsIgnore)
            metadata[ReplayEditor.METADATA_FLAGS_OFFSET] |= ReplayEditor.FLAG_SANITIZE_FRIENDSIGNORES;
        if (Scraper.sanitizeVersion != -1 && editor.getReplayVersion().version != Scraper.sanitizeVersion)
            metadata[ReplayEditor.METADATA_FLAGS_OFFSET] |= ReplayEditor.FLAG_SANITIZE_VERSION;
    }

    public void processReplay(String fname) {

        ReplayEditor editor = new ReplayEditor();
        setFlags(editor);
        boolean success = false;
        int keyCRC = 0;

        if (editor.importNonPacketData(fname)) {
            keyCRC = editor.getKeyCRC();
            String visitedReplay = Scraper.m_replaysKeysProcessed.get(keyCRC);
            if (visitedReplay != null) {
                Logger.Warn("@|red Skipping replay |@@|yellow " + fname + "|@@|red , seems to be a duplicate of |@@|green " + visitedReplay + "|@");
                return;
            } else {
                Scraper.m_replaysKeysProcessed.put(keyCRC, fname);
            }

            success = editor.importPacketData(fname);
        }

        if (!success) {
            Logger.Warn("Replay " + fname + " is not valid, skipping");
            return;
        }

        Logger.Info("@|cyan Started sanitizing |@@|white [" + keyCRC + "]|@ aka "+ fname);

        Logger.Info("@|white [" + keyCRC + "]|@ client version: " + editor.getReplayVersion().clientVersion);
        Logger.Info("@|white [" + keyCRC + "]|@ replay version: " + editor.getReplayVersion().version);

        if (!Settings.sanitizeForce && !editor.authenticReplay()) {
            Logger.Warn("Replay " + keyCRC + " is not an authentic rsc replay. skipping");
            return;
        }

        Logger.Debug(fname);

        int localPID = -1;
        int playerX = -1;
        int playerY = -1;
        int playerAnim = -1;
        int planeX = -1;
        int planeY = -1;
        int planeFloor = -1;
        int floorYOffset = -1;
        int length = -1;

        if (Settings.dumpMessages || Settings.dumpChat) {
            Scraper.m_replayDictionarySQL.add(
                String.format("%s%d', '%s');\n",
                    "INSERT INTO `rscMessages`.`replayDictionary` (`index`, `filePath`) VALUES ('",
                    keyCRC,
                    fname.replaceFirst(Settings.sanitizePath, "").replaceAll("'", "''")
                )
            );
        }

        int op234type1EchoCount = 0;
        int sendPMEchoCount = 0;
        for (int slot = 0; slot < 30; slot++) {
            inventoryItems[slot] = -1;
            inventoryStackAmounts[slot] = -1;
            inventoryItemEquipped[slot] = -1;
        }
        for (int itemID = 0; itemID < 1290; itemID++) {
            inventoryItemsAllCount[itemID] = 0;
        }

        HashMap<Integer, ReplayPacket> interestingSleepPackets = new HashMap<Integer, ReplayPacket>();

        // Process incoming packet
        LinkedList<ReplayPacket> incomingPackets = editor.getIncomingPackets();
        for (ReplayPacket packet : incomingPackets) {
            Logger.Debug("@|white [" + keyCRC + "]|@ " + String.format("incoming opcode: %d",packet.opcode));
            try {

                switch (packet.opcode) {
                    case ReplayEditor.VIRTUAL_OPCODE_CONNECT:
                        Logger.Info("@|white [" + keyCRC + "]|@ loginresponse: " + packet.data[0] + " (timestamp: " + packet.timestamp + ")");

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
                                Scraper.m_messageSQL.add(
                                    "INSERT INTO `rscMessages`.`SEND_MESSAGE` (`replayIndex`, `timestamp`, `messageType`, `infoContained`, `message`, `sender`, `sender2`, `color`) VALUES " +
                                        String.format("('%d', '%d', '%d', '%d', '%s', '%s', '%s', '%s');\n",
                                            keyCRC,
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
                            String dialogueOptionsValues = String.format("'%d', '%d', '%d'", editor.getKeyCRC(), packet.timestamp, numberOfOptions);

                            for (int i = 1; i <= numberOfOptions; i++) {
                                dialogueOptionsInsert = String.format("%s%s%d%s",dialogueOptionsInsert, ", `choice", i, "`");
                                dialogueOptionsValues += String.format(", '%s'", packet.readPaddedString().replaceAll("'", "''"));
                            }

                            Scraper.m_messageSQL.add( String.format("%s%s%s%s",
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
                        playerAnim = packet.readBitmask(4);
                        packet.readBitmask(4);
                        packet.endBitmask();
                        if (Settings.dumpScenery) {
                            fillView(playerX, playerY, sceneryLocs);
                        }
                        packet.skip(packet.data.length - 4);
                        break;
                    case PacketBuilder.OPCODE_UPDATE_PLAYERS: { // 234
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
                                    Scraper.m_chatSQL.add(
                                        "INSERT INTO `rscMessages`.`UPDATE_PLAYERS_TYPE_1` (`replayIndex`, `timestamp`, `pid`, `localPlayerMessageCount`, `message`) VALUES " +
                                            String.format("('%d', '%d', '%d', '%d', '%s');\n",
                                                keyCRC,
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
                            } else if (updateType == 2) { // damage taken
                                packet.skip(3);
                            } else if (updateType == 3) { // show projectile towards an NPC
                                int sprite = packet.readUnsignedShort();
                                int shooterIndex = packet.readUnsignedShort();
                                if (Settings.dumpNPCDamage) {
                                    if (npcsServer[shooterIndex] != null) {
                                        npcsServer[shooterIndex].attackingPlayerServerIndex = pid;
                                        npcsServer[shooterIndex].incomingProjectileSprite = sprite;
                                        npcsServer[shooterIndex].lastAttackerIndex = pid;
                                        npcsServer[shooterIndex].lastSprite = sprite;
                                    } else {
                                        // NPC possibly off screen & being shot by player closer to the NPC.
                                    }
                                }
                            } else if (updateType == 4) { // show projectile towards a player
                                int sprite = packet.readUnsignedShort();
                                int shooterIndex = packet.readUnsignedShort();
                                if (Settings.dumpNPCDamage) {
                                    if (sprite != 3) { // gnome ball
                                        Logger.Info("@|white [" + keyCRC + "]|@ " + pid + " shot at " + shooterIndex + " with " + sprite);
                                    }
                                }
                            } else if (updateType == 5) { // equipment change
                                packet.skip(2);
                                packet.readPaddedString();
                                packet.readPaddedString();
                                int equipCount = packet.readUnsignedByte();
                                packet.skip(equipCount);
                                packet.skip(6);
                            } else if (updateType == 6) { // quest chat
                                String message = packet.readRSCString();
                                if (Settings.dumpMessages) {
                                    Scraper.m_messageSQL.add(
                                        "INSERT INTO `rscMessages`.`UPDATE_PLAYERS_TYPE_6` (`replayIndex`, `timestamp`, `pid`, `message`) VALUES " +
                                            String.format("('%d', '%d', '%d', '%s');\n",
                                                keyCRC,
                                                packet.timestamp,
                                                pid,
                                                message.replaceAll("'", "''")
                                            )
                                    );
                                }
                            } else {
                                Logger.Info("@|white [" + keyCRC + "]|@ Hit unanticipated update type " + updateType);
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
                                        editor.getReplayMetadata().IPAddress4 =worldNum;
                                    } else {
                                        int worldNumExcluded = Integer.parseInt(world.substring(world.length() - 1));
                                        if (worldNumExcluded <= 5) {
                                            editor.getReplayMetadata().world_num_excluded |= (int)Math.pow(2,worldNumExcluded - 1);
                                        } else {
                                            editor.foundInauthentic = true;
                                            Logger.Warn("@|white [" + keyCRC + "]|@ Inauthentic amount of worlds");
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Logger.Error("@|white [" + keyCRC + "]|@ " + String.format("error parsing opcode_update_friend, packet.timestamp: %d", packet.timestamp));
                        }
                        break;
                    case PacketBuilder.OPCODE_RECV_PM:
                        if (Settings.dumpChat) {
                            String recvPMSender1 = packet.readPaddedString(); //sender's name
                            String recvPMSender2 = packet.readPaddedString(); //sender's name again
                            Boolean recvPMSenderTwice = recvPMSender1.equals(recvPMSender2);
                            if (!recvPMSenderTwice) {
                                Logger.Warn("@|white [" + keyCRC + "]|@ Sender1 != Sender2 in OPCODE_RECV_PM!");
                            }
                            int recvPMModeratorStatus = packet.readByte();
                            BigInteger recvPMMessageID = packet.readUnsignedLong();
                            String recvPMMessage = packet.readRSCString();

                            Scraper.m_chatSQL.add(
                                "INSERT INTO `rscMessages`.`RECEIVE_PM` (`replayIndex`, `timestamp`, `sendersRepeated`, `moderator`, `messageID`, `message`) VALUES " +
                                    String.format("('%d', '%d', '%s', '%d', '%d', '%s');\n",
                                        keyCRC,
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
                            Scraper.m_chatSQL.add(
                                "INSERT INTO `rscMessages`.`SEND_PM_SERVER_ECHO` (`replayIndex`, `timestamp`, `messageCount`, `message`) VALUES " +
                                    String.format("('%d', '%d', '%d', '%s');\n",
                                        keyCRC,
                                        packet.timestamp,
                                        sendPMEchoCount++,
                                        sendPMMessage.replaceAll("'", "''")
                                    )
                            );
                        }
                        if (Settings.sanitizePrivateChat)
                            packet.opcode = ReplayEditor.VIRTUAL_OPCODE_NOP;
                        break;
                    case PacketBuilder.OPCODE_CREATE_NPC: // 79
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
                                            if (Settings.dumpNPCDamage) {
                                                if (npc == null) continue;
                                                if (removalIsKillshot(npc, localPID, playerX, playerY)) {
                                                    Logger.Info("@|white [" + keyCRC + "]|@ killshot!!! PID " + npc.lastAttackerIndex + " killed NPC Index " + npc.serverIndex + " @ timestamp:" + packet.timestamp);

                                                    if (npc.attackingPlayerServerIndex != localPID) {
                                                        npc.incomingProjectileSprite = npc.lastSprite;
                                                    }
                                                    // npc was under attack at time of death, this is a kill shot
                                                    switch (npc.incomingProjectileSprite) {
                                                        case -1:
                                                            Logger.Info("@|white [" + keyCRC + "]|@ incoming projectile sprite not set, but npc was damaged while not in melee combat");
                                                            break;
                                                        case 2: // ranged projectile

                                                            // best case scenario. We know who is attacking, that it is definitely ranged, and all stats

                                                            // determine bow wielded; might not exist if spear, dart, throwing knife
                                                            int bow = -1;
                                                            for (int slot = 0; slot < 30; slot++) {
                                                                if (inventoryItemEquipped[slot] == 1) {
                                                                    switch (inventoryItems[slot]) {
                                                                        case 59:
                                                                        case 60:
                                                                        case 188:
                                                                        case 189:
                                                                        case 648:
                                                                        case 649:
                                                                        case 650:
                                                                        case 651:
                                                                        case 652:
                                                                        case 653:
                                                                        case 654:
                                                                        case 655:
                                                                        case 656:
                                                                        case 657:
                                                                            bow = inventoryItems[slot];
                                                                    }
                                                                }
                                                            }

                                                            Scraper.m_damageSQL.add(
                                                                "INSERT INTO `rscDamage`.`unambiguousRanged` (`replayPath`, `timestamp`, `npcId`, `damageTaken`, `currentHP`, `maxHP`, `lastAmmo`, `bow`, `curRangedStat`, `killshot`) VALUES " +
                                                                    String.format("('%s', '%d', '%d', '%d', '%d', '%d', '%d', '%d', '%d', '%d');\n",
                                                                        fname.replaceFirst(Settings.sanitizePath, "").replaceAll("'", "''"),
                                                                        packet.timestamp,
                                                                        npc.npcId,
                                                                        npc.healthCurrent, // damage taken, assumed to be current health
                                                                        npc.healthCurrent,
                                                                        npc.healthMax,
                                                                        lastAmmo, // arrow/bolt/knife/dart/spear was determined in inventory updates
                                                                        bow,
                                                                        playerCurStat[4], // ranged level
                                                                        1 // killshot
                                                                    )
                                                            );


                                                            break;
                                                        case 3: // gnomeball
                                                            Logger.Info("@|white [" + keyCRC + "]|@ What in tarnation? enemy killed by gnomeball??");
                                                            break;
                                                        case 1: // magic spell
                                                        case 4: // iban blast
                                                        case 6: // god spell
                                                            break;
                                                        case 5: // cannonball
                                                            break;
                                                        default:
                                                            Logger.Info("@|white [" + keyCRC + "]|@ unknown projectile, shouldn't be possible;; timestamp: " + packet.timestamp);
                                                            break;
                                                    }

                                                    npc.attackingPlayerServerIndex = -1;
                                                    npc.incomingProjectileSprite = -1;
                                                }
                                            }

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
                                    Scraper.m_npcLocCSV.add( String.format("%s,%d,%f,%d,%d,%d,%d\n",
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
                    case PacketBuilder.OPCODE_UPDATE_NPC: // 104
                        if (Settings.dumpMessages || Settings.dumpNPCDamage) {
                            int updateNpcCount = packet.readUnsignedShort();
                            for (int i = 0; i < updateNpcCount; i++) {
                                int npcServerIndex = packet.readUnsignedShort();
                                int updateType = packet.readByte();
                                if (updateType == 1) { // npc chat
                                    int pidTalkingTo = packet.readUnsignedShort();
                                    String updateNPCMessage = packet.readRSCString();
                                    if (Settings.dumpMessages) {
                                        Scraper.m_messageSQL.add(
                                            "INSERT INTO `rscMessages`.`UPDATE_NPCS_TYPE_1` (`replayIndex`, `timestamp`, `npcId`, `pidTalkingTo`, `message`) VALUES " +
                                                String.format("('%d', '%d', '%d', '%d', '%s');\n",
                                                    keyCRC,
                                                    packet.timestamp,
                                                    npcsServer[npcServerIndex].npcId,
                                                    pidTalkingTo,
                                                    updateNPCMessage.replaceAll("'", "''")
                                                )
                                        );
                                    }
                                } else if (updateType == 2) { // combat update
                                    int npcDamageTaken = packet.readUnsignedByte();
                                    int npcCurrentHP = packet.readUnsignedByte();
                                    int npcMaxHP = packet.readUnsignedByte();
                                    npcsServer[npcServerIndex].healthCurrent = npcCurrentHP;
                                    npcsServer[npcServerIndex].healthMax = npcMaxHP;

                                    if (Settings.dumpNPCDamage) {
                                        // determine how the npc is being attacked
                                        if (npcsServer[npcServerIndex] != null) {

                                            if (npcsServer[npcServerIndex].animationNext >= 8) {
                                                // in melee combat, but not necessarily have taken melee damage, could be spell
                                                if (npcsServer[npcServerIndex].incomingProjectileSprite != -1) {
                                                    // TODO: can we assume that this means that this specific damage update was in fact ranged/mage damage?
                                                    npcsServer[npcServerIndex].attackingPlayerServerIndex = -1;
                                                    npcsServer[npcServerIndex].incomingProjectileSprite = -1;
                                                }

                                                // determine who is possibly attacking

                                                // need to check if local player shares same X & Y coordinate as this NPC & has melee stance
                                                if (playerX == npcsServer[npcServerIndex].currentX && playerY == npcsServer[npcServerIndex].currentY && playerAnim >= 8) {
                                                    // possible & likely that player is the same player attacking NPC, need to check there aren't 2 fights on the same tile
                                                }
                                            } else {
                                                // not possible for this to be melee damage :-)
                                                switch (npcsServer[npcServerIndex].incomingProjectileSprite) {
                                                    case -1:
                                                        Logger.Info("@|white [" + keyCRC + "]|@ incoming projectile sprite not set, but npc was damaged while not in melee combat;; timestamp: " + packet.timestamp);
                                                        break;
                                                    case 1: // magic projectile
                                                        if (npcsServer[npcServerIndex].attackingPlayerServerIndex == localPID) {
                                                            // must determine the spell that was used
                                                            // must determine mage level & magic bonus
                                                        } else {
                                                            // useless, since we don't know what spell was used
                                                        }
                                                        break;
                                                    case 2: // ranged projectile
                                                        if (npcsServer[npcServerIndex].attackingPlayerServerIndex == localPID) {
                                                            // best case scenario. We know who is attacking, that it is definitely ranged, and all stats

                                                            // determine bow wielded; might not exist if spear, dart, throwing knife
                                                            int bow = -1;
                                                            for (int slot = 0; slot < 30; slot++) {
                                                                if (inventoryItemEquipped[slot] == 1) {
                                                                    switch (inventoryItems[slot]) {
                                                                        case 59:
                                                                        case 60:
                                                                        case 188:
                                                                        case 189:
                                                                        case 648:
                                                                        case 649:
                                                                        case 650:
                                                                        case 651:
                                                                        case 652:
                                                                        case 653:
                                                                        case 654:
                                                                        case 655:
                                                                        case 656:
                                                                        case 657:
                                                                            bow = inventoryItems[slot];
                                                                    }
                                                                }
                                                            }

                                                            Scraper.m_damageSQL.add(
                                                                "INSERT INTO `rscDamage`.`unambiguousRanged` (`replayPath`, `timestamp`, `npcId`, `damageTaken`, `currentHP`, `maxHP`, `lastAmmo`, `bow`, `curRangedStat`, `killshot`) VALUES " +
                                                                    String.format("('%s', '%d', '%d', '%d', '%d', '%d', '%d', '%d', '%d');\n",
                                                                        fname.replaceFirst(Settings.sanitizePath, "").replaceAll("'", "''"),
                                                                        packet.timestamp,
                                                                        npcsServer[npcServerIndex].npcId,
                                                                        npcDamageTaken,
                                                                        npcCurrentHP,
                                                                        npcMaxHP,
                                                                        lastAmmo, // arrow/bolt/knife/dart/spear was determined in inventory updates
                                                                        bow,
                                                                        playerCurStat[4], // ranged level
                                                                        0 // not a kill shot
                                                                    )
                                                            );

                                                        } else {
                                                            // much less useful, since we don't know player's ranged level.
                                                            // arrow could still be sometimes determined if player doesn't pick it up
                                                            // and then we could maybe see the expected distribution of hits for some unknown level
                                                        }
                                                        break;
                                                    case 3: // gnomeball
                                                        break;
                                                    case 4: // iban blast
                                                        break;
                                                    case 5: // cannonball
                                                        break;
                                                    case 6: // god spell
                                                        break;
                                                    default:
                                                        Logger.Info("@|white [" + keyCRC + "]|@ unknown projectile, shouldn't be possible;; timestamp: " + packet.timestamp);
                                                        break;
                                                }

                                                npcsServer[npcServerIndex].attackingPlayerServerIndex = -1;
                                                npcsServer[npcServerIndex].incomingProjectileSprite = -1;
                                            }
                                        } else {
                                            // some other player possibly shooting an an NPC off-screen that we don't know about?
                                        }
                                    }
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
                                        Logger.Error("@|white [" + keyCRC + "]|@ Invalid region or not logged in; Aborting");
                                        break;
                                    }

                                    if (!validCoordinates(x, y)) {
                                        Logger.Error("@|white [" + keyCRC + "]|@ Invalid coordinates " + x + ", " + y + "; Aborting");
                                        break;
                                    } else if (type != 60000 && !sceneryIDBlacklisted(type, x, y)) {
                                        if (type < 0 || type > 1188) {
                                            Logger.Error("@|white [" + keyCRC + "]|@ Scenery id " + type + " at " + x + ", " + y + " is invalid; Aborting");
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
                                        Logger.Error("@|white [" + keyCRC + "]|@ Invalid region or not logged in; Aborting");
                                        break;
                                    }

                                    if (!validCoordinates(x, y)) {
                                        Logger.Error("@|white [" + keyCRC + "]|@ Invalid coordinates " + x + ", " + y + "; Aborting");
                                        break;
                                    } else if (type != 0xFFFF && !boundaryIDBlacklisted(type, x, y)) {
                                        if (type < 0 || type > 213) {
                                            Logger.Error("@|white [" + keyCRC + "]|@ Boundary id " + type + " at " + x + ", " + y + " is invalid; Aborting");
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
                        } else if (Settings.checkBoundaryRemoval) {
                            while (length > 0) {
                                if (packet.readUnsignedByte() == 255) {
                                    int x = playerX + packet.readByte();
                                    int y = playerY + packet.readByte();
                                    if (Scraper.worldManager.getViewArea(x, y).getBoundary(x, y) != null) {
                                        Logger.Info("@|white [" + keyCRC + "]|@ " + String.format("@|red BOUNDARY REMOVAL ACTUALLY DID SOMETHING @ %d,%d IN REPLAY %s AT TIMESTAMP %d|@", x, y, fname, packet.timestamp));
                                    }
                                    length -= 3;
                                } else {
                                    length -= 5;
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
                    case PacketBuilder.OPCODE_SHOW_SHOP:
                        if (Settings.dumpShops) {
                            int shopItemCount = packet.readUnsignedByte();
                            byte shopType = packet.readByte();
                            int sellpricemod = packet.readUnsignedByte();
                            int buypricemod = packet.readUnsignedByte();
                            int priceMultiplier = packet.readUnsignedByte();
                            for (int index = 0; index < shopItemCount; ++ index) {
                                int itemId = packet.readUnsignedShort();
                                int itemCount = packet.readUnsignedShort();
                                int itemPrice = packet.readUnsignedShort();

                                if (itemCount > highestStoreStock) {
                                    Logger.Info("@|white [" + keyCRC + "]|@ New max amount found: " + itemCount + " in replay " + fname);
                                    highestStoreStock = itemCount;
                                }
                            }
                        }
                        break;
                    case PacketBuilder.OPCODE_SET_INVENTORY: // 53
                        if (Settings.dumpInventories || Settings.dumpNPCDamage) {
                            int inventoryItemCount = packet.readUnsignedByte();

                            inventoryItemsAllCount = new int[1290];
                            for (int i=0; i < 1290; i++) {
                                inventoryItemsAllCount[i] = 0;
                            }

                            for (int slot = 0; slot < inventoryItemCount; slot++) {
                                int itemIDAndEquipped = packet.readUnsignedShort();
                                int equipped = ((itemIDAndEquipped >> 15) & 0x1);
                                int itemID = itemIDAndEquipped & 0x7FFF;
                                long itemStack = 1;
                                if (JGameData.itemStackable[itemID]) {
                                    itemStack = packet.readUnsignedInt3();
                                }
                                if (Settings.dumpInventories) {
                                    Scraper.m_inventorySQL.add(
                                        "INSERT INTO `rscMessages`.`INVENTORIES` (`replayIndex`, `timestamp`, `slot`, `equipped`, `itemID`, `amount`, `opcode`) VALUES " +
                                            String.format("('%s', '%d', '%d', '%d', '%d', '%d', '%d');\n",
                                                keyCRC,
                                                packet.timestamp,
                                                slot,
                                                equipped,
                                                itemID,
                                                itemStack,
                                                packet.opcode)
                                    );
                                }

                                inventoryItemsAllCount[itemID] += itemStack;

                                inventoryItems[slot] = itemID;
                                inventoryStackAmounts[slot] = itemStack;
                                inventoryItemEquipped[slot] = equipped;
                            }
                        }
                        break;
                    case PacketBuilder.OPCODE_SET_INVENTORY_SLOT: // 90
                        if (packet.data == null) {
                            break;
                        }

                        if (Settings.dumpInventories || Settings.dumpNPCDamage) {
                            int slot = packet.readUnsignedByte();
                            int itemIDAndEquipped = packet.readUnsignedShort();
                            int equipped = ((itemIDAndEquipped >> 15) & 0x1);
                            int itemID = itemIDAndEquipped & 0x7FFF;
                            long itemStack = 1;
                            if (JGameData.itemStackable[itemID]) {
                                itemStack = packet.readUnsignedInt3();
                            }

                            if (Settings.dumpInventories) {
                                Scraper.m_inventorySQL.add(
                                    "INSERT INTO `rscMessages`.`INVENTORIES` (`replayIndex`, `timestamp`, `slot`, `equipped`, `itemID`, `amount`, `opcode`) VALUES " +
                                        String.format("('%s', '%d', '%d', '%d', '%d', '%d', '%d');\n",
                                            keyCRC,
                                            packet.timestamp,
                                            slot,
                                            equipped,
                                            itemID,
                                            itemStack,
                                            packet.opcode)
                                );
                            }

                            inventoryItems[slot] = itemID;
                            inventoryStackAmounts[slot] = itemStack;
                            inventoryItemEquipped[slot] = equipped;

                            if (Settings.dumpNPCDamage) {
                                if (isAmmo(itemID)) {
                                    lastAmmo = itemID;
                                }
                            }
                        }
                        break;

                    case PacketBuilder.OPCODE_REMOVE_INVENTORY_SLOT: // 123
                        int slotRemoved = packet.readUnsignedByte();
                        int itemID = inventoryItems[slotRemoved];

                        inventoryItemsAllCount[itemID] -= inventoryStackAmounts[slotRemoved];
                        for (int slot = slotRemoved; slot < 28; slot++) {
                            inventoryItems[slot] = inventoryItems[slot + 1];
                            inventoryStackAmounts[slot] = inventoryStackAmounts[slot + 1];
                            inventoryItemEquipped[slot] = inventoryItemEquipped[slot + 1];
                        }

                        if (Settings.dumpNPCDamage) {
                            if (isAmmo(itemID)) {
                                lastAmmo = itemID;
                            }
                        }

                        break;

                    case PacketBuilder.OPCODE_SET_STATS: // 156
                        for (int stat = 0; stat < 17; stat++) {
                            playerCurStat[stat] = packet.readByte();
                        }
                        for (int stat = 0; stat < 17; stat++) {
                            playerBaseStat[stat] = packet.readByte();
                        }
                        for (int stat = 0; stat < 17; stat++) {
                            playerXP[stat] = packet.readUnsignedInt();
                        }
                        packet.skip(1); // quest points
                        break;

                    case PacketBuilder.OPCODE_UPDATE_STAT: // 159
                        int stat = packet.readByte();
                        playerCurStat[stat] = packet.readByte();
                        playerBaseStat[stat] = packet.readByte();
                        playerXP[stat] = packet.readUnsignedInt();
                        break;

                    case PacketBuilder.OPCODE_PLAY_SOUND: // 204
                        lastSound = packet.readPaddedString();
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
                if (Scraper.m_sceneryLocs.containsKey(key)) {
                    int oldID = Scraper.m_sceneryLocs.get(key);
                    if (oldID == SCENERY_BLANK)
                        continue;
                    if (id == SCENERY_BLANK && oldID != SCENERY_BLANK && sceneryIDRemoveList(oldID, getPackedX(key), getPackedY(key))) {
                        Scraper.m_sceneryLocs.put(key, id);
                        continue;
                    }
                    if (id != SCENERY_BLANK) {
                        id = handleSceneryIDConflict(Scraper.m_sceneryLocs.get(key), id);
                        Scraper.m_sceneryLocs.put(key, id);
                    }
                } else {
                    Scraper.m_sceneryLocs.put(key, id);
                }
            }
        }
        if (Settings.dumpBoundaries) {
            for (HashMap.Entry<Integer, Integer> entry : boundaryLocs.entrySet()) {
                int key = entry.getKey();
                int value = entry.getValue();
                if (Scraper.m_boundaryLocs.containsKey(key))
                    value = handleBoundaryIDConflict(Scraper.m_boundaryLocs.get(key), value);
                Scraper.m_boundaryLocs.put(key, value);
            }
        }

        int op216Count = 0;
        int sendPMCount = 0;

        // Process outgoing packets
        LinkedList<ReplayPacket> outgoingPackets = editor.getOutgoingPackets();
        for (ReplayPacket packet : outgoingPackets) {
            Logger.Debug("@|white [" + keyCRC + "]|@ " + String.format("outgoing opcode: %d",packet.opcode));
            try {
                switch (packet.opcode) {
                    case ReplayEditor.VIRTUAL_OPCODE_CONNECT: // Login
                        Logger.Info("@|white [" + keyCRC + "]|@ outgoing login (timestamp: " + packet.timestamp + ")");
                        break;

                    case 216: // Send chat message
                        if (Settings.dumpChat) {
                            String sendChatMessage = packet.readRSCString();
                            Scraper.m_chatSQL.add(
                                "INSERT INTO `rscMessages`.`SEND_CHAT` (`replayIndex`, `timestamp`, `pid`, `sendCount`, `message`) VALUES " +
                                    String.format("('%d', '%d', '%d', '%d', '%s');\n",
                                        editor.getKeyCRC(),
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
                            Scraper.m_chatSQL.add(
                                "INSERT INTO `rscMessages`.`SEND_PM` (`replayIndex`, `timestamp`, `messageCount`, `message`) VALUES " +
                                    String.format("('%d', '%d', '%d', '%s');\n",
                                        keyCRC,
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
                            Scraper.m_messageSQL.add(
                                String.format("INSERT INTO `rscMessages`.`CLIENT_CHOOSE_DIALOGUE_OPTION` (`replayIndex`, `timestamp`, `choice`) VALUES ('%d', '%d', '%d');\n",
                                    keyCRC,
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

                    case 235: // Send appearance
                        if (Settings.dumpAppearances) {
                            System.out.print("Appearance Packet in " + fname + ": ");
                            try {
                                for (int i = 0; i < 8; i++) {
                                    System.out.print(String.format("%d ", packet.readUnsignedByte()));
                                }
                            } catch (ArrayIndexOutOfBoundsException e) {
                                System.out.print(" XXXXX");
                            }
                            System.out.println();
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
                Logger.Info("@|white [" + keyCRC + "]|@ " + String.format("Timestamp: %d; Opcode: %d;",
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
                                        Logger.Info("@|white [" + keyCRC + "]|@ Found guess: " + sleepWordGuess);
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

                            Logger.Info("@|white [" + keyCRC + "]|@ " + String.format("sleepword %d: %d length: %d", cur, packet.opcode, packet.data.length));

                        } else {
                            Logger.Warn("Zero length packet 117 in " + fname);
                        }
                        break;
                }

            }
        }

        if (Settings.sanitizeReplays) {
            // Set exported replay version
            if (Scraper.sanitizeVersion != -1)
                editor.getReplayVersion().version = Scraper.sanitizeVersion;

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
        Logger.Info("@|cyan,intensity_bold Finished sanitizing |@@|white [" + keyCRC + "]|@ aka " + fname );
    }
}
