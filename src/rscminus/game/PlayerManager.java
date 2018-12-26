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

import rscminus.game.data.LoginInfo;
import rscminus.game.data.SaveInfo;
import rscminus.game.entity.Player;

import java.nio.channels.SocketChannel;

public class PlayerManager {
    private Player m_players[];
    private QueuedPlayer m_queue[];
    private NetworkStream m_stream;
    private WorldManager m_worldManager;
    private int m_size;

    public PlayerManager() {
        m_stream = new NetworkStream();
    }

    public void init(int size) {
        m_size = size;
        m_players = new Player[m_size];
        m_queue = new QueuedPlayer[m_size];

        m_worldManager = Server.getInstance().getWorldManager();
        for (int i = 0; i < m_size; i++) {
            m_players[i] = new Player(i, this, m_worldManager);
            m_queue[i] = new QueuedPlayer();

            m_players[i].reset();
            m_queue[i].reset();
        }
    }

    public void addQueuedPlayer(SocketChannel socket) {
        int slot = findFreeQueuedSlot();
        if (slot != -1) {
            m_queue[slot].setSocket(socket);
            m_queue[slot].setActive(true);
            System.out.println("queued slot: " + slot);
            return;
        }

        // Reject login if we have no free slots
        m_stream.flip();
        m_stream.writeUnsignedByte(QueuedPlayer.LOGIN_REJECT);
        m_stream.flush(socket);
    }

    public int addPlayer(SocketChannel socket, LoginInfo loginInfo, SaveInfo saveInfo) {
        int loggedSlot = findPlayerByUsername(loginInfo.username);

        if (loginInfo.reconnecting && loggedSlot != -1) {
            if (m_players[loggedSlot].getSocket() != null)
                return QueuedPlayer.LOGIN_LOGGED_IN;
            m_players[loggedSlot].setLoginInfo(loginInfo);
            m_players[loggedSlot].setSocket(socket);
            return QueuedPlayer.LOGIN_SUCCESS;
        }

        if (loggedSlot != -1)
            return QueuedPlayer.LOGIN_LOGGED_IN;

        int slot = findFreeSlot();
        if (slot != -1) {
            System.out.println("add slot: " + slot);
            m_players[slot].setLoginInfo(loginInfo);
            m_players[slot].setSaveInfo(saveInfo);
            m_players[slot].setSocket(socket);
            m_players[slot].setActive(true);
            return QueuedPlayer.LOGIN_SUCCESS;
        }

        return QueuedPlayer.LOGIN_FULL;
    }

    public void removePlayer(int index) {
        if (m_players[index].isActive()) {
            // TODO: Handle saving
            m_worldManager.removePlayer(m_players[index]);
            m_players[index].reset();
            System.out.println("remove slot: " + index);
        }
    }

    public int findPlayerByUsername(String username) {
        for (int i = 0; i < m_size; i++) {
            if (m_players[i].isActive() && m_players[i].getUsername().equals(username))
                return i;
        }
        return -1;
    }

    public int findFreeQueuedSlot() {
        for (int i = 0; i < m_size; i++) {
            if (!m_queue[i].isActive())
                return i;
        }
        return -1;
    }

    public int findFreeSlot() {
        for (int i = 0; i < m_size; i++) {
            if (!m_players[i].isActive())
                return i;
        }
        return -1;
    }

    public void processLoginQueue() {
        for (int i = 0; i < m_size; i++) {
            if (m_queue[i].isActive())
                m_queue[i].process();
        }
    }

    public void processIncomingPackets() {
        for (int i = 0; i < m_size; i++) {
            if (m_players[i].isActive())
                m_players[i].processIncomingPackets();
        }
    }

    public void processOutgoingPackets() {
        for (int i = 0; i < m_size; i++) {
            if (m_players[i].isActive())
                m_players[i].processOutgoingPackets();
        }
    }

    public void process() {
        for (int i = 0; i < m_size; i++) {
            if (m_players[i].isActive())
                m_players[i].process();
        }
    }

    public void processClientUpdate() {
        for (int i = 0; i < m_size; i++) {
            if (m_players[i].isActive())
                m_players[i].processClientUpdate();
        }
    }

    public void processDisconnect() {
        for (int i = 0; i < m_size; i++) {
            if (m_players[i].isActive())
                m_players[i].processDisconnect();
        }
    }
}
