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

package rscminus.game;

import rscminus.common.ISAACCipher;
import rscminus.common.JGameData;
import rscminus.common.SleepWord;
import rscminus.game.constants.Game;
import rscminus.game.data.SaveInfo;
import rscminus.game.entity.Scenery;
import rscminus.game.entity.Player;
import rscminus.game.entity.Boundary;

public class PacketBuilder {
    public static final int OPCODE_CLOSE_CONNECTION_NOTIFY = 4;
    public static final int OPCODE_QUEST_STATUS = 5;
    public static final int OPCODE_FLOOR_SET = 25;
    public static final int OPCODE_UPDATE_XP = 33;
    public static final int OPCODE_SCENERY_HANDLER = 48;
    public static final int OPCODE_PRIVACY_SETTINGS = 51;
    public static final int OPCODE_SET_INVENTORY = 53;
    public static final int OPCODE_SET_APPEARANCE = 59;
    public static final int OPCODE_CREATE_NPC = 79;
    public static final int OPCODE_WAKE_UP = 84;
    public static final int OPCODE_SEND_PM = 87;
    public static final int OPCODE_SET_INVENTORY_SLOT = 90;
    public static final int OPCODE_BOUNDARY_HANDLER = 91;
    public static final int OPCODE_GROUNDITEM_HANDLER = 99;
    public static final int OPCODE_SHOW_SHOP = 101;
    public static final int OPCODE_UPDATE_NPC = 104;
    public static final int OPCODE_SET_IGNORE = 109;
    public static final int OPCODE_SKIP_TUTORIAL = 111;
    public static final int OPCODE_SET_FATIGUE = 114;
    public static final int OPCODE_SLEEP_WORD = 117;
    public static final int OPCODE_RECV_PM = 120;
    public static final int OPCODE_REMOVE_INVENTORY_SLOT = 123;
    public static final int OPCODE_SEND_MESSAGE = 131;
    public static final int OPCODE_UPDATE_FRIEND = 149;
    public static final int OPCODE_SET_EQUIP_STATS = 153;
    public static final int OPCODE_SET_STATS = 156;
    public static final int OPCODE_UPDATE_STAT = 159;
    public static final int OPCODE_LOGOUT = 165;
    public static final int OPCODE_SHOW_WELCOME = 182;
    public static final int OPCODE_DENY_LOGOUT = 183;
    public static final int OPCODE_CREATE_PLAYERS = 191;
    public static final int OPCODE_PLAY_SOUND = 204;
    public static final int OPCODE_SET_PRAYERS = 206;
    public static final int OPCODE_UPDATE_PLAYERS = 234;
    public static final int OPCODE_UPDATE_IGNORE = 237;
    public static final int OPCODE_GAME_SETTINGS = 240;
    public static final int OPCODE_DIALOGUE_OPTIONS = 245;

    public static String currentSleepDataFilename;

    private static int m_offset = 0;
    private static int m_count = 0;

    public static void questStatus(boolean questComplete[], NetworkStream stream, ISAACCipher isaacCipher) {
        stream.startPacket();
        stream.writeOpcode(OPCODE_QUEST_STATUS, isaacCipher);
        for (int i = 0; i < questComplete.length; i++)
            stream.writeByte(questComplete[i] ? (byte)1 : (byte)0);
        stream.endPacket();
    }

    public static void setFloor(int id, int floor, NetworkStream stream, ISAACCipher isaacCipher) {
        stream.startPacket();
        stream.writeOpcode(OPCODE_FLOOR_SET, isaacCipher);
        stream.writeUnsignedShort(id);
        stream.writeUnsignedShort(Game.WORLD_PLANE_X);
        stream.writeUnsignedShort(Game.WORLD_PLANE_Y);
        stream.writeUnsignedShort(floor);
        stream.writeUnsignedShort(Game.WORLD_Y_OFFSET);
        stream.endPacket();
    }

    public static void privacySettings(boolean blockChat, boolean blockPrivate, boolean blockTrade, boolean blockDuel, NetworkStream stream, ISAACCipher isaacCipher) {
        stream.startPacket();
        stream.writeOpcode(OPCODE_PRIVACY_SETTINGS, isaacCipher);
        stream.writeUnsignedByte(blockChat ? 1 : 0);
        stream.writeUnsignedByte(blockPrivate ? 1 : 0);
        stream.writeUnsignedByte(blockTrade ? 1 : 0);
        stream.writeUnsignedByte(blockDuel ? 1 : 0);
        stream.endPacket();
    }

    public static void setInventory(SaveInfo saveInfo, NetworkStream stream, ISAACCipher isaacCipher) {
        stream.startPacket();
        stream.writeOpcode(OPCODE_SET_INVENTORY, isaacCipher);
        stream.writeUnsignedByte(saveInfo.inventoryCount);
        for (int i = 0; i < saveInfo.inventoryCount; i++) {
            int mod = saveInfo.inventory[i] & 0x7FFF;
            mod |= (saveInfo.inventoryEquipped[i] ? 1 : 0) << 15;
            stream.writeUnsignedShort(mod);
            if (JGameData.itemStackable[saveInfo.inventory[i]])
                stream.writeUnsignedShortInt(saveInfo.inventoryStack[i]);
        }
        stream.endPacket();
    }

    public static void setAppearance(NetworkStream stream, ISAACCipher isaacCipher) {
        stream.startPacket();
        stream.writeOpcode(OPCODE_SET_APPEARANCE, isaacCipher);
        stream.endPacket();
    }

    public static void setInventorySlot(int slot, SaveInfo saveInfo, NetworkStream stream, ISAACCipher isaacCipher) {
        stream.startPacket();
        stream.writeOpcode(OPCODE_SET_INVENTORY_SLOT, isaacCipher);
        stream.writeUnsignedByte(slot);
        int mod = saveInfo.inventory[slot] & 0x7FFF;
        mod |= (saveInfo.inventoryEquipped[slot] ? 1 : 0) << 15;
        stream.writeUnsignedShort(mod);
        if (JGameData.itemStackable[saveInfo.inventory[slot]])
            stream.writeUnsignedShortInt(saveInfo.inventoryStack[slot]);
        stream.endPacket();
    }

    public static void addGroundItem(Player player, int x, int y, int id, NetworkStream stream, ISAACCipher isaacCipher) {
        stream.startPacket();
        stream.writeOpcode(OPCODE_GROUNDITEM_HANDLER, isaacCipher);
        stream.writeUnsignedShort(id);
        stream.writeUnsignedByte(player.getX() - x);
        stream.writeUnsignedByte(player.getY() - y);
        stream.endPacket();
    }

    public static void startSceneryUpdate(NetworkStream stream, ISAACCipher isaacCipher) {
        stream.startPacket();
        stream.writeOpcode(OPCODE_SCENERY_HANDLER, isaacCipher);
    }

    public static void addSceneryUpdate(Player player, Scenery scenery, NetworkStream stream, ISAACCipher isaacCipher) {
        int x = scenery.getX() - player.getX();
        int y = scenery.getY() - player.getY();
        stream.writeUnsignedShort(scenery.getID());
        stream.writeByte((byte)x);
        stream.writeByte((byte)y);
    }

    public static void endSceneryUpdate(NetworkStream stream) {
        stream.endPacket();
    }

    public static void startBoundaryUpdate(NetworkStream stream, ISAACCipher isaacCipher) {
        stream.startPacket();
        stream.writeOpcode(OPCODE_BOUNDARY_HANDLER, isaacCipher);
    }

    public static void addBoundaryUpdate(Player player, Boundary boundary, NetworkStream stream, ISAACCipher isaacCipher) {
        int x = boundary.getX() - player.getX();
        int y = boundary.getY() - player.getY();
        stream.writeUnsignedShort(boundary.getID());
        stream.writeByte((byte)x);
        stream.writeByte((byte)y);
        stream.writeByte((byte)boundary.getDirection());
    }

    public static void endBoundaryUpdate(NetworkStream stream) {
        stream.endPacket();
    }

    public static void skipTutorial(boolean show, NetworkStream stream, ISAACCipher isaacCipher) {
        stream.startPacket();
        stream.writeOpcode(OPCODE_SKIP_TUTORIAL, isaacCipher);
        stream.writeUnsignedByte(show ? 1 : 0);
        stream.endPacket();
    }

    public static void setFatigue(SaveInfo saveInfo, NetworkStream stream, ISAACCipher isaacCipher) {
        stream.startPacket();
        stream.writeOpcode(OPCODE_SET_FATIGUE, isaacCipher);
        stream.writeUnsignedShort(saveInfo.fatigue);
        stream.endPacket();
    }

    public static void removeInventorySlot(int slot, NetworkStream stream, ISAACCipher isaacCipher) {
        stream.startPacket();
        stream.writeOpcode(OPCODE_REMOVE_INVENTORY_SLOT, isaacCipher);
        stream.writeUnsignedByte(slot);
        stream.endPacket();
    }

    public static void sendMessage(int type, String message, String sender, String clan, String color, NetworkStream stream, ISAACCipher isaacCipher) {
        // TODO: Check params

        int mask = 0;
        if (sender != null || clan != null)
            mask |= 1;
        if (color != null)
            mask |= 2;

        stream.startPacket();
        stream.writeOpcode(OPCODE_SEND_MESSAGE, isaacCipher);
        stream.writeUnsignedByte(type);
        stream.writeUnsignedByte(mask);
        stream.writePaddedString(message);
        if ((mask & 1) != 0) {
            stream.writePaddedString(sender);
            stream.writePaddedString(clan);
        }
        if ((mask & 2) != 0)
            stream.writePaddedString(color);
        stream.endPacket();
    }

    public static void setEquipStats(int equipStats[], NetworkStream stream, ISAACCipher isaacCipher) {
        stream.startPacket();
        stream.writeOpcode(OPCODE_SET_EQUIP_STATS, isaacCipher);
        for (int i = 0; i < equipStats.length; i++)
            stream.writeUnsignedByte(equipStats[i]);
        stream.endPacket();
    }

    public static void updateStat(int skill, int effectiveLevel, int baseLevel, int xp, NetworkStream stream, ISAACCipher isaacCipher) {
        stream.startPacket();
        stream.writeOpcode(OPCODE_UPDATE_STAT, isaacCipher);
        stream.writeUnsignedByte(skill);
        stream.writeUnsignedByte(effectiveLevel);
        stream.writeUnsignedByte(baseLevel);
        stream.writeUnsignedInt(xp);
        stream.endPacket();
    }

    public static void setStats(SaveInfo saveInfo, int questPoints, NetworkStream stream, ISAACCipher isaacCipher) {
        stream.startPacket();
        stream.writeOpcode(OPCODE_SET_STATS, isaacCipher);
        for (int i = 0; i < Game.STAT_COUNT; i++)
            stream.writeUnsignedByte(saveInfo.statCurrent[i]);
        for (int i = 0; i < Game.STAT_COUNT; i++)
            stream.writeUnsignedByte(saveInfo.statMax[i]);
        for (int i = 0; i < Game.STAT_COUNT; i++)
            stream.writeUnsignedInt(saveInfo.statXP[i]);
        stream.writeUnsignedByte(questPoints);
        stream.endPacket();
    }

    public static void updateXP(int skill, int xp, NetworkStream stream, ISAACCipher isaacCipher) {
        stream.startPacket();
        stream.writeOpcode(OPCODE_UPDATE_XP, isaacCipher);
        stream.writeUnsignedByte(skill);
        stream.writeUnsignedInt(xp);
        stream.endPacket();

    }

    public static void logout(NetworkStream stream, ISAACCipher isaacCipher) {
        stream.startPacket();
        stream.writeOpcode(OPCODE_LOGOUT, isaacCipher);
        stream.endPacket();
    }

    public static void showWelcome(int ip, int lastLoginDays, int recoverySetDays, int unreadMessages, NetworkStream stream, ISAACCipher isaacCipher) {
        stream.startPacket();
        stream.writeOpcode(OPCODE_SHOW_WELCOME, isaacCipher);
        stream.writeUnsignedInt(ip);
        stream.writeUnsignedShort(lastLoginDays);
        stream.writeUnsignedByte(recoverySetDays);
        stream.writeUnsignedShort(unreadMessages);
        stream.endPacket();
    }

    public static void denyLogout(NetworkStream stream, ISAACCipher isaacCipher) {
        stream.startPacket();
        stream.writeOpcode(OPCODE_DENY_LOGOUT, isaacCipher);
        stream.endPacket();
    }

    public static void startCreatePlayers(Player player, NetworkStream stream, ISAACCipher isaacCipher) {
        stream.startPacket();
        stream.writeOpcode(OPCODE_CREATE_PLAYERS, isaacCipher);
        stream.startBitmask();
        stream.writeBitmask(11, player.getX()); // Local region X
        stream.writeBitmask(13, player.getY()); // Local region Y
        stream.writeBitmask(4, player.getDirection()); // Anim
        m_offset = stream.getBitPosition();
        stream.writeBitmask(8, 0); // Players
        m_count = 0;
    }

    public static void endCreatePlayers(NetworkStream stream) {
        int endPosition = stream.getBitPosition();
        stream.setBitPosition(m_offset);
        stream.writeBitmask(8, m_count);
        stream.setBitPosition(endPosition);
        stream.endBitmask();
        stream.endPacket();
    }

    public static void addCreatePlayers(Player player, Player otherPlayer, NetworkStream stream) {
        int x = otherPlayer.getX() - player.getX();
        int y = otherPlayer.getY() - player.getY();
        stream.writeBitmask(11, otherPlayer.getIndex()); // Local region X
        stream.writeBitmask(5, x); // Local region X
        stream.writeBitmask(5, y); // Local region Y
        stream.writeBitmask(4, otherPlayer.getDirection()); // Anim
    }

    public static void playSound(String sound, NetworkStream stream, ISAACCipher isaacCipher) {
        stream.startPacket();
        stream.writeOpcode(OPCODE_PLAY_SOUND, isaacCipher);
        stream.writePaddedString(sound);
        stream.endPacket();
    }

    public static void setPrayers(boolean prayers[], NetworkStream stream, ISAACCipher isaacCipher) {
        stream.startPacket();
        stream.writeOpcode(OPCODE_SET_PRAYERS, isaacCipher);
        for (int i = 0; i < prayers.length; i++)
            stream.writeUnsignedByte(prayers[i] ? 1 : 0);
        stream.endPacket();
    }

    public static void startPlayerUpdate(NetworkStream stream, ISAACCipher isaacCipher) {
        stream.startPacket();
        stream.writeOpcode(OPCODE_UPDATE_PLAYERS, isaacCipher);
        m_offset = stream.getPosition();
        stream.writeUnsignedShort(0); // Player count
        m_count = 0;
    }

    public static void addPlayerUpdateChat(Player player, NetworkStream stream) {
        stream.writeUnsignedShort(player.getIndex());
        stream.writeUnsignedByte(1); // Update type
        //TODO: Implement MOD status
        stream.writeUnsignedByte(0); // Mod status
        stream.writeVariableSize(player.chatMessage.decipheredLength);
        int sizeToWrite = player.chatMessage.encipheredLength;
        if (stream.getAvailable() < sizeToWrite)
            sizeToWrite = stream.getAvailable();
        stream.writeArray(player.chatMessage.messageBuffer, 0, sizeToWrite);
        ++m_count;
    }

    public static void addPlayerUpdateAppearance(Player player, NetworkStream stream) {
        SaveInfo saveInfo = player.getSaveInfo();
        stream.writeUnsignedShort(player.getIndex()); // Index
        stream.writeUnsignedByte(5); // Update type
        stream.writeUnsignedShort(player.getIndex()); // Server Index
        stream.writePaddedString(player.getUsername()); // Display name
        stream.writePaddedString(player.getUsername()); // Account name
        stream.writeUnsignedByte(3);
        stream.writeUnsignedByte(1 + saveInfo.headType); // Equipment Count
        stream.writeUnsignedByte(1 + saveInfo.top); // Equipment Count
        stream.writeUnsignedByte(1 + saveInfo.bottom); // Equipment Count
        stream.writeUnsignedByte(saveInfo.hairColour); // Hair color
        stream.writeUnsignedByte(saveInfo.topColour); // Top color
        stream.writeUnsignedByte(saveInfo.bottomColour); // Bottom color
        stream.writeUnsignedByte(saveInfo.skinColour); // Skin color
        stream.writeUnsignedByte(3); // Level
        stream.writeUnsignedByte(0); // Skull
        ++m_count;
    }

    public static void endPlayerUpdate(NetworkStream stream) {
        stream.endPacket();
        int endPosition = stream.getPosition();
        stream.setPosition(m_offset);
        stream.writeUnsignedShort(m_count);
        stream.setPosition(endPosition);
    }

    public static void gameSettings(boolean cameraAuto, boolean mouseOneButton, boolean soundDisabled, NetworkStream stream, ISAACCipher isaacCipher) {
        stream.startPacket();
        stream.writeOpcode(OPCODE_GAME_SETTINGS, isaacCipher);
        stream.writeUnsignedByte(cameraAuto ? 1 : 0);
        stream.writeUnsignedByte(mouseOneButton ? 1 : 0);
        stream.writeUnsignedByte(soundDisabled ? 1 : 0);
        stream.endPacket();
    }
    public static void wakeUp(NetworkStream stream, ISAACCipher isaacCipher) {
        stream.startPacket();
        stream.writeOpcode(OPCODE_WAKE_UP, isaacCipher);
        stream.endPacket();
    }

    public static void sendSleepWord(SleepWord sleepData, NetworkStream stream, ISAACCipher isaacCipher) {
        stream.startPacket();
        stream.writeOpcode(OPCODE_SLEEP_WORD, isaacCipher);
        currentSleepDataFilename = sleepData.filename;
        int sizeToWrite = sleepData.cheaterData.length;
        if (stream.getAvailable() < sizeToWrite)
            sizeToWrite = stream.getAvailable();
        stream.writeArray(sleepData.cheaterData, 0, sizeToWrite);
        stream.endPacket();
    }
}
