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

package rscminus.game.entity;

import rscminus.common.ISAACCipher;
import rscminus.common.JGameData;
import rscminus.common.SocketUtil;
import rscminus.common.StringEncryption;
import rscminus.game.*;
import rscminus.game.constants.Game;
import rscminus.game.data.LoginInfo;
import rscminus.game.data.SaveInfo;
import rscminus.game.entity.player.ActionSlot;
import rscminus.game.entity.player.WalkingQueue;
import rscminus.game.world.ViewRegion;

import java.nio.channels.SocketChannel;

public class Player extends Entity {
    private NetworkStream m_incomingStream;
    private NetworkStream m_outgoingStream;
    private NetworkStream m_packetStream;
    private LoginInfo m_loginInfo;
    private SaveInfo m_saveInfo;
    private ISAACCipher m_isaacIncoming;
    private ISAACCipher m_isaacOutgoing;
    private PlayerManager m_playerManager;
    private WorldManager m_worldManager;
    private SocketChannel m_socket;

    // Server update variables
    private ViewRegion m_viewRegion;
    private ActionSlot m_actionSlot;

    // Tick update variables
    private boolean m_tickRequestLogout;
    private boolean m_tickUpdateFloor;

    // Packet opcodes
    public static final int OPCODE_INTERACT_WALLOBJECT_OPTION1 = 14;
    public static final int OPCODE_WALKTO_SOURCE = 16;
    public static final int OPCODE_DISCONNECT = 31;
    public static final int OPCODE_KEEPALIVE = 67;
    public static final int OPCODE_INTERACT_OBJECT_OPTION2 = 79;
    public static final int OPCODE_LOGOUT = 102;
    public static final int OPCODE_INTERACT_WALLOBJECT_OPTION2 = 127;
    public static final int OPCODE_INTERACT_OBJECT_OPTION1 = 136;
    public static final int OPCODE_EQUIP_ITEM = 169;
    public static final int OPCODE_UNEQUIP_ITEM = 170;
    public static final int OPCODE_WALKTO = 187;
    public static final int OPCODE_SET_APPEARANCE = 235;
    public static final int OPCODE_DROP_ITEM = 246;
    public static final int OPCODE_PUBLIC_CHAT = 216;

    // Player update
    private boolean m_updateChat;
    private boolean m_updateAppearance;

    // Game state
    private int m_index;
    private int m_direction;
    private boolean m_questComplete[];
    private int m_questPoints;
    private int m_equipmentStats[];
    private boolean m_prayers[];
    private boolean m_tutorial;
    private boolean m_loggedIn;
    private boolean m_loggedOut;
    private WalkingQueue m_walkingQueue;

    public byte[] chatMessage = new byte[256];

    public Player(int index, PlayerManager playerManager, WorldManager worldManager) {
        m_index = index;
        m_playerManager = playerManager;
        m_worldManager = worldManager;
        m_incomingStream = new NetworkStream();
        m_outgoingStream = new NetworkStream();
        m_packetStream = new NetworkStream();
        m_isaacIncoming = new ISAACCipher();
        m_isaacOutgoing = new ISAACCipher();
        m_equipmentStats = new int[Game.EQUIP_STAT_COUNT];
        m_prayers = new boolean[Game.PRAYER_COUNT];
        m_questComplete = new boolean[Game.QUEST_COUNT];
        m_walkingQueue = new WalkingQueue();
        m_actionSlot = new ActionSlot();
        m_viewRegion = new ViewRegion(m_worldManager);
    }

    public void reset() {
        m_incomingStream.flip();
        m_outgoingStream.flip();
        m_packetStream.flip();
        m_tutorial = false;
        m_loggedIn = false;
        m_loggedOut = false;
        m_isaacOutgoing.reset();
        m_isaacIncoming.reset();
        setActive(false);
        setSocket(null);

        // Server state
        m_walkingQueue.clear();
        m_viewRegion.clear();
        m_direction = Game.DIRECTION_NORTH;

        // Game state
        m_tickRequestLogout = false;
        m_tickUpdateFloor = true;

        // Player update
        m_updateAppearance = true;
        m_updateChat = false;
    }

    public void setLoginInfo(LoginInfo loginInfo) {
        m_loginInfo = loginInfo;
        m_isaacIncoming.setKeys(m_loginInfo.keys);
        m_isaacOutgoing.setKeys(m_loginInfo.keys);
        m_loggedIn = false;
    }

    public void setSaveInfo(SaveInfo saveInfo) {
        m_saveInfo = saveInfo;
    }

    public SocketChannel getSocket() {
        return m_socket;
    }

    public String getUsername() {
        return m_loginInfo.username;
    }

    public SaveInfo getSaveInfo() {
        return  m_saveInfo;
    }

    public int getX() {
        return m_saveInfo.x;
    }

    public int getY() {
        return m_saveInfo.y;
    }

    public int getDirection() {
        return m_direction;
    }

    public int getFloor() {
        return m_saveInfo.y / Game.WORLD_Y_OFFSET;
    }

    public int getIndex() {
        return m_index;
    }

    public ISAACCipher getISAACCipher() {
        return m_isaacOutgoing;
    }

    public NetworkStream getNetworkStream() {
        return m_outgoingStream;
    }

    public void addX(int x) {
        setX(m_saveInfo.x + x);
    }

    public void addY(int y) {
        setY(m_saveInfo.y + y);
    }

    public void addFloor(int floor) {
        addY(floor * Game.WORLD_Y_OFFSET);
    }

    public void setX(int x) {
        m_saveInfo.x = x;
    }

    public void setY(int y) {
        int prevFloor = getFloor();
        m_saveInfo.y = y;
        if (m_saveInfo.y < 0)
            m_saveInfo.y = Game.WORLD_HEIGHT + m_saveInfo.y;
        else if (m_saveInfo.y > Game.WORLD_HEIGHT)
            m_saveInfo.y -= Game.WORLD_HEIGHT;
        if (getFloor() != prevFloor)
            m_tickUpdateFloor = true;
    }

    public void setDirection(int direction) {
        m_direction = direction;
    }

    public void closeSocket() {
        if (m_socket != null) {
            SocketUtil.close(m_socket);
            m_socket = null;
        }
    }

    public void setSocket(SocketChannel socket) {
        if (m_socket != null)
            SocketUtil.close(m_socket);
        m_socket = socket;
    }

    public void sendClientState() {
        PacketBuilder.privacySettings(false, false, false, false, m_outgoingStream, m_isaacOutgoing);
        PacketBuilder.sendMessage(Game.CHAT_QUEST, "Welcome to " + Server.getInstance().getName() + "!", null, null, null, m_outgoingStream, m_isaacOutgoing);
        PacketBuilder.gameSettings(false, false, false, m_outgoingStream, m_isaacOutgoing);
        PacketBuilder.setFloor(m_index, getFloor(), m_outgoingStream, m_isaacOutgoing);
        PacketBuilder.questStatus(m_questComplete, m_outgoingStream, m_isaacOutgoing);
        PacketBuilder.skipTutorial(m_tutorial, m_outgoingStream, m_isaacOutgoing);
        PacketBuilder.showWelcome(0x7F000001, 0, Game.WELCOME_RECOVERY_UNSET, Game.WELCOME_MESSAGES_SHOW, m_outgoingStream, m_isaacOutgoing);
        m_saveInfo.inventoryCount = 11;
        m_saveInfo.inventory[0] = 124;
        m_saveInfo.inventoryStack[0] = 1;
        m_saveInfo.inventoryEquipped[0] = false;
        m_saveInfo.inventory[1] = 197;
        m_saveInfo.inventoryStack[1] = 1;
        m_saveInfo.inventoryEquipped[1] = false;
        m_saveInfo.inventory[2] = 199;
        m_saveInfo.inventoryStack[2] = 1;
        m_saveInfo.inventoryEquipped[2] = false;
        m_saveInfo.inventory[3] = 216;
        m_saveInfo.inventoryStack[3] = 1;
        m_saveInfo.inventoryEquipped[3] = false;
        m_saveInfo.inventory[4] = 389;
        m_saveInfo.inventoryStack[4] = 1;
        m_saveInfo.inventoryEquipped[4] = false;
        m_saveInfo.inventory[5] = 556;
        m_saveInfo.inventoryStack[5] = 1;
        m_saveInfo.inventoryEquipped[5] = false;
        m_saveInfo.inventory[6] = 722;
        m_saveInfo.inventoryStack[6] = 1;
        m_saveInfo.inventoryEquipped[6] = false;
        m_saveInfo.inventory[7] = 726;
        m_saveInfo.inventoryStack[7] = 1;
        m_saveInfo.inventoryEquipped[7] = false;
        m_saveInfo.inventory[8] = 1213;
        m_saveInfo.inventoryStack[8] = 1;
        m_saveInfo.inventoryEquipped[8] = false;
        m_saveInfo.inventory[9] = 121;
        m_saveInfo.inventoryStack[9] = 1;
        m_saveInfo.inventoryEquipped[9] = false;
        m_saveInfo.inventory[10] = 117;
        m_saveInfo.inventoryStack[10] = 1;
        m_saveInfo.inventoryEquipped[10] = false;
        PacketBuilder.setInventory(m_saveInfo, m_outgoingStream, m_isaacOutgoing);

        // Repeat setting floor unnecessarily b/c it is authentic
        PacketBuilder.setFloor(m_index, getFloor(), m_outgoingStream, m_isaacOutgoing);

        PacketBuilder.setStats(m_saveInfo, m_questPoints, m_outgoingStream, m_isaacOutgoing);
        PacketBuilder.setEquipStats(m_equipmentStats, m_outgoingStream, m_isaacOutgoing);
        PacketBuilder.setPrayers(m_prayers, m_outgoingStream, m_isaacOutgoing);
        PacketBuilder.setFatigue(m_saveInfo, m_outgoingStream, m_isaacOutgoing);
        PacketBuilder.setAppearance(m_outgoingStream, m_isaacOutgoing);
    }

    public void playSound(String name) {
        // Server sends sound even if the user sound settings is off
        PacketBuilder.playSound(name, m_outgoingStream, m_isaacOutgoing);
    }

    public boolean canLogout() {
        return true;
    }

    public void removeInventoryItem(int index) {
        // Index is to high
        if (index >= m_saveInfo.inventoryCount)
            return;

        m_saveInfo.inventoryCount--;
        for(int i = index; i < m_saveInfo.inventoryCount; i++) {
            m_saveInfo.inventory[i] = m_saveInfo.inventory[i + 1];
            m_saveInfo.inventoryEquipped[i] = m_saveInfo.inventoryEquipped[i + 1];
            m_saveInfo.inventoryStack[i] = m_saveInfo.inventoryStack[i + 1];
        }
        PacketBuilder.removeInventorySlot(index, m_outgoingStream, m_isaacOutgoing);
    }

    public void dropInventoryItem(int index) {
        // Index is to high
        if (index >= m_saveInfo.inventoryCount)
            return;

        PacketBuilder.addGroundItem(this, getX(), getY(), m_saveInfo.inventory[index], m_outgoingStream, m_isaacOutgoing);

        removeInventoryItem(index);
        playSound("dropobject");
    }

    public void equipInventoryItem(int index) {
        // Item is already equipped or index is to high
        if (index >= m_saveInfo.inventoryCount || m_saveInfo.inventoryEquipped[index])
            return;
        int wearable = JGameData.itemWearable[m_saveInfo.inventory[index]];
        // Item isn't wearable
        if (wearable == 0)
            return;
        // TODO: Check level requirements

        // Remove items with conflicting mask
        for (int i = 0; i < m_saveInfo.inventoryCount; i++) {
            if (m_saveInfo.inventoryEquipped[i] && (JGameData.itemWearable[m_saveInfo.inventory[i]] & wearable) > 0) {
                m_saveInfo.inventoryEquipped[i] = false;
                PacketBuilder.setInventorySlot(i, m_saveInfo, m_outgoingStream, m_isaacOutgoing);
            }
        }

        // Equip item
        m_saveInfo.inventoryEquipped[index] = true;
        PacketBuilder.setInventorySlot(index, m_saveInfo, m_outgoingStream, m_isaacOutgoing);
        playSound("click");
    }

    public void unequipInventoryItem(int index) {
        // Item isn't equipped or index is to high
        if (index >= m_saveInfo.inventoryCount || !m_saveInfo.inventoryEquipped[index])
            return;

        m_saveInfo.inventoryEquipped[index] = false;
        PacketBuilder.setInventorySlot(index, m_saveInfo, m_outgoingStream, m_isaacOutgoing);
        playSound("click");
    }

    public void interactBoundary(Boundary boundary, int option) {
        if (option == 0)
            interactBoundaryOption1(boundary);
        else if (option == 1)
            interactBoundaryOption2(boundary);

        // Clear action slot and walking queue
        m_actionSlot.clear();
        m_walkingQueue.clear();
    }

    public void interactBoundaryOption1(Boundary boundary) {
        int id = boundary.getID();
        switch (id) {
        case 2: // Door
            boundary.setID(1);
            playSound("opendoor");
            break;
        case 8: // Door
            boundary.setID(9);
            playSound("opendoor");
            break;
        default:
            System.out.println("Unhandled Boundary interaction, id: " + id + ", option: 1");
            break;
        }
    }

    public void interactBoundaryOption2(Boundary boundary) {
        int id = boundary.getID();
        switch (id) {
        case 1: // Door
            boundary.setID(2);
            playSound("closedoor");
            break;
        case 9: // Door
            boundary.setID(8);
            playSound("closedoor");
            break;
        default:
            System.out.println("Unhandled Boundary interaction, id: " + id + ", option: 2");
            break;
        }
    }

    public void interactScenery(Scenery scenery, int option) {
        if (option == 0)
            interactSceneryOption1(scenery);
        else if (option == 1)
            interactSceneryOption2(scenery);

        // Clear action slot and walking queue
        m_actionSlot.clear();
        m_walkingQueue.clear();
    }

    public void interactSceneryOption1(Scenery scenery) {
        int id = scenery.getID();
        switch (id) {
        case 5: // Ladder
            addFloor(1);
            break;
        case 6: // Ladder
            addFloor(-1);
            break;
        case 60: // gate
            scenery.setID(59);
            break;
        case 64: // doors
            scenery.setID(63);
            break;
        default:
            System.out.println("Unhandled Scenery interaction, id: " + id + ", direction: " + scenery.getDirection() + ", option: 1");
            break;
        }
    }

    public void interactSceneryOption2(Scenery scenery) {
        int id = scenery.getID();
        switch (id) {
        case 59: // gate
            scenery.setID(60);
            break;
        case 63: // doors
            scenery.setID(64);
            break;
        default:
            System.out.println("Unhandled Scenery interaction, id: " + id + ", direction: " + scenery.getDirection() + ", option: 2");
            break;
        }
    }

    public void process() {
        if (!m_loggedIn) {
            sendClientState();
            m_loggedIn = true;
        }

        // Process async action slot first
        processAsyncActionSlot();

        // Process walking
        if (m_walkingQueue.update(getX(), getY())) {
            int nextX = m_walkingQueue.getNextX();
            int nextY = m_walkingQueue.getNextY();
            if(!m_worldManager.checkCollision(getX(), getY(), nextX, nextY)) {
                addX(nextX);
                addY(nextY);
                setDirection(m_walkingQueue.getNextDirection());
            } else {
                m_walkingQueue.clear();
                // Process action slot, we were stopped
                processActionSlot();
            }
        } else {
            // Process action slot if not walking
            processActionSlot();
        }

        // Update region if required
        if (m_tickUpdateFloor) {
            PacketBuilder.setFloor(m_index, getFloor(), m_outgoingStream, m_isaacOutgoing);
            m_tickUpdateFloor = false;
        }

        // Update view region
        m_viewRegion.update(this);
    }

    private void processAsyncActionSlot() {
        int action = m_actionSlot.getAction();

        if (action == ActionSlot.ACTION_NONE)
            return;

        switch (action) {
        case ActionSlot.ACTION_INTERACT_WALLOBJECT:
        {
            int x = m_actionSlot.getInteractX();
            int y = m_actionSlot.getInteractY();
            int direction = m_actionSlot.getInteractDirection();
            int option = m_actionSlot.getInteractOption();
            m_worldManager.interactBoundary(this, x, y, direction, option);
            break;
        }
        case ActionSlot.ACTION_INTERACT_OBJECT:
        {
            int x = m_actionSlot.getInteractX();
            int y = m_actionSlot.getInteractY();
            int option = m_actionSlot.getInteractOption();
            m_worldManager.interactScenery(this, x, y, option);
            break;
        }
        case ActionSlot.ACTION_WALK:
            m_actionSlot.getWalkingQueue().copyTo(m_walkingQueue);
            m_actionSlot.clear();
            break;
        case ActionSlot.ACTION_INVENTORY_EQUIP:
            equipInventoryItem(m_actionSlot.getInventorySlot());
            m_actionSlot.clear();
            break;
        case ActionSlot.ACTION_INVENTORY_UNEQUIP:
            unequipInventoryItem(m_actionSlot.getInventorySlot());
            m_actionSlot.clear();
            break;
        }
    }

    private void processActionSlot() {
        int action = m_actionSlot.getAction();

        if (action == ActionSlot.ACTION_NONE)
            return;

        switch (action) {
        case ActionSlot.ACTION_INVENTORY_DROP:
            dropInventoryItem(m_actionSlot.getInventorySlot());
            m_actionSlot.clear();
            break;
        }
    }

    public void processPlayerUpdate(NetworkStream stream) {
        if (m_updateAppearance)
            PacketBuilder.addPlayerUpdateAppearance(this, stream);
        if (m_updateChat)
            PacketBuilder.addPlayerUpdateChat(this, stream);
    }

    public void processClientUpdate() {
        m_viewRegion.sendUpdate(this);
    }

    public void processDisconnect() {
        if (!canLogout()) {
            if (m_socket != null && m_tickRequestLogout) {
                PacketBuilder.denyLogout(m_outgoingStream, m_isaacOutgoing);
                m_tickRequestLogout = false;
            }
            return;
        }

        // User is disconnected
        if (m_socket == null) {
            m_loggedOut = true;
            return;
        }

        if (m_tickRequestLogout) {
            PacketBuilder.logout(m_outgoingStream, m_isaacOutgoing);
            m_loggedOut = true;
        }
    }

    public void processIncomingPackets() {
        m_incomingStream.fill(m_socket);

        int length;
        while ((length = m_incomingStream.readPacket(m_packetStream)) > 0) {
            int opcode = m_packetStream.readOpcode(m_isaacIncoming);

            // Handle incoming packets
            switch (opcode) {
            case OPCODE_INTERACT_WALLOBJECT_OPTION1:
            case OPCODE_INTERACT_WALLOBJECT_OPTION2:
            {
                int x = m_packetStream.readShort();
                int y = m_packetStream.readShort();
                int direction = m_packetStream.readByte();
                int option = (opcode == OPCODE_INTERACT_WALLOBJECT_OPTION1) ? 0 : 1;
                m_actionSlot.setAction(ActionSlot.ACTION_INTERACT_WALLOBJECT);
                m_actionSlot.setInteraction(x, y, direction, option);
                break;
            }
            case OPCODE_WALKTO_SOURCE:
            {
                int startX = m_packetStream.readUnsignedShort();
                int startY = m_packetStream.readUnsignedShort();
                if (startX == getX() && startY == getY())
                    break;
                m_walkingQueue.clear();
                m_walkingQueue.add(startX, startY);
                int waypoints = (length - 5) / 2;
                for (int i = 0; i < waypoints; i++) {
                    int x = startX + m_packetStream.readByte();
                    int y = startY + m_packetStream.readByte();
                    m_walkingQueue.add(x, y);
                }
                break;
            }
            case OPCODE_DISCONNECT:
                closeSocket();
                break;
            case OPCODE_KEEPALIVE:
                break;
            case OPCODE_LOGOUT:
                m_tickRequestLogout = true;
                break;
            case OPCODE_INTERACT_OBJECT_OPTION1:
            case OPCODE_INTERACT_OBJECT_OPTION2:
            {
                int x = m_packetStream.readShort();
                int y = m_packetStream.readShort();
                int option = (opcode == OPCODE_INTERACT_OBJECT_OPTION1) ? 0 : 1;
                m_actionSlot.setAction(ActionSlot.ACTION_INTERACT_OBJECT);
                m_actionSlot.setInteraction(x, y, option);
                break;
            }
            case OPCODE_EQUIP_ITEM:
                m_actionSlot.setAction(ActionSlot.ACTION_INVENTORY_EQUIP);
                m_actionSlot.setInventorySlot(m_packetStream.readUnsignedShort());
                break;
            case OPCODE_UNEQUIP_ITEM:
                m_actionSlot.setAction(ActionSlot.ACTION_INVENTORY_UNEQUIP);
                m_actionSlot.setInventorySlot(m_packetStream.readUnsignedShort());
                break;
            case OPCODE_WALKTO:
            {
                int startX = m_packetStream.readUnsignedShort();
                int startY = m_packetStream.readUnsignedShort();
                if (startX == getX() && startY == getY())
                    break;
                m_actionSlot.setAction(ActionSlot.ACTION_WALK);
                m_actionSlot.getWalkingQueue().clear();
                m_actionSlot.getWalkingQueue().add(startX, startY);
                int waypoints = (length - 5) / 2;
                for (int i = 0; i < waypoints; i++) {
                    int x = startX + m_packetStream.readByte();
                    int y = startY + m_packetStream.readByte();
                    m_actionSlot.getWalkingQueue().add(x, y);
                }
                break;
            }
            case OPCODE_SET_APPEARANCE:
            {
                int gender = m_packetStream.readUnsignedByte();
                int headType = m_packetStream.readUnsignedByte();
                int top = m_packetStream.readUnsignedByte();
                int bottom = m_packetStream.readUnsignedByte();
                int hairColor = m_packetStream.readUnsignedByte();
                int topColor = m_packetStream.readUnsignedByte();
                int bottomColor = m_packetStream.readUnsignedByte();
                int skinColor = m_packetStream.readUnsignedByte();
                m_saveInfo.headType = headType;
                m_saveInfo.top = top;
                m_saveInfo.bottom = bottom;
                m_saveInfo.hairColor = hairColor;
                m_saveInfo.topColor = topColor;
                m_saveInfo.bottomColor = bottomColor;
                m_saveInfo.skinColor = skinColor;
                m_updateAppearance = true;
                break;
            }
            case OPCODE_DROP_ITEM:
                m_actionSlot.setAction(ActionSlot.ACTION_INVENTORY_DROP);
                m_actionSlot.setInventorySlot(m_packetStream.readUnsignedShort());
                break;
            case OPCODE_PUBLIC_CHAT:
                int messageLength = m_packetStream.readVariableSize();

                if (messageLength <= 0 || messageLength > 80)
                    break;

                String message = StringEncryption.decipher(m_packetStream, messageLength);
                //TODO: Profanity filter
                StringEncryption.encipher(chatMessage, message);
                m_updateChat = true;
                break;
            default:
                System.out.println("undefined opcode: " + opcode + ", length: " + length);
                while (length-- > 0) {
                    System.out.print(m_packetStream.readByte() + " ");
                }
                System.out.println();
                break;
            }
        }
    }

    public void processOutgoingPackets() {
        m_outgoingStream.flush(m_socket);

        // Reset player update states
        m_updateAppearance = false;
        m_updateChat = false;

        // Player is logged out, remove them from the player list
        if (m_loggedOut)
            m_playerManager.removePlayer(m_index);
    }
}
