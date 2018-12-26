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

import rscminus.common.SocketUtil;
import rscminus.game.data.LoginInfo;
import rscminus.game.data.SaveInfo;

import java.nio.channels.SocketChannel;

public class QueuedPlayer {
    private boolean m_active;
    private SocketChannel m_socket;
    private NetworkStream m_stream;
    private NetworkStream m_packetStream;
    private PlayerManager m_playerManager;

    // Login response constants
    public static final int LOGIN_ACCOUNT_INVALID = 3;
    public static final int LOGIN_LOGGED_IN = 4;
    public static final int LOGIN_UPDATE = 5;
    public static final int LOGIN_REJECT = 8;
    public static final int LOGIN_FULL = 14;
    public static final int LOGIN_SUCCESS = 64;

    public QueuedPlayer() {
        m_playerManager = Server.getInstance().getPlayerManager();
        m_stream = new NetworkStream();
        m_packetStream = new NetworkStream();
        m_active = false;
        m_socket = null;
    }

    public void reset() {
        m_stream.flip();
        m_packetStream.flip();
        m_active = false;
        m_socket = null;
    }

    public boolean isActive() {
        return m_active;
    }

    public void setSocket(SocketChannel socket) {
        m_socket = socket;
    }

    public void setActive(boolean active) {
        m_active = active;
    }

    private int handleLogin(LoginInfo loginInfo, SaveInfo saveInfo) {
        int opcode = m_packetStream.readUnsignedByte();
        if (opcode != 0)
            return LOGIN_REJECT;

        // Handle login packet
        loginInfo.reconnecting = (m_packetStream.readUnsignedByte() == 1);
        int version = m_packetStream.readUnsignedInt();

        if (version != 235)
            return LOGIN_UPDATE;

        // Decrypt login block
        int length = m_packetStream.readData(m_stream);
        m_stream.decryptRSA(length);

        // Handle login block
        opcode = m_stream.readUnsignedByte();
        if (opcode != 10)
            return LOGIN_REJECT;
        loginInfo.keys[0] = m_stream.readUnsignedInt();
        loginInfo.keys[1] = m_stream.readUnsignedInt();
        loginInfo.keys[2] = m_stream.readUnsignedInt();
        loginInfo.keys[3] = m_stream.readUnsignedInt();
        String password = m_stream.readUnicodeString().trim();
        // Decrypt XTEA block
        length = m_packetStream.readUnsignedShort();
        m_packetStream.decryptXTEA(length, loginInfo.keys);

        // Handle XTEA block
        m_packetStream.skip(25);
        String username = m_packetStream.readUnicodeString();

        // TODO: We would check and grab account information here
        loginInfo.username = username;

        return LOGIN_SUCCESS;
    }

    public void process() {
        m_stream.fill(m_socket);

        // TODO: Handle timeout

        if (m_stream.readPacket(m_packetStream) == 0)
            return;

        LoginInfo loginInfo = new LoginInfo();
        SaveInfo saveInfo = new SaveInfo();
        int loginResponse = handleLogin(loginInfo, saveInfo);

        // Successful login
        if (loginResponse == LOGIN_SUCCESS) {
            loginResponse = m_playerManager.addPlayer(m_socket, loginInfo, saveInfo);

            // TODO: Add mod priviledges
            if (loginResponse == LOGIN_SUCCESS) {
            }
        }

        m_stream.flip();
        m_stream.writeUnsignedByte(loginResponse);
        m_stream.flush(m_socket);

        // Session denied, close the socket
        if ((loginResponse & LOGIN_SUCCESS) == 0)
            SocketUtil.close(m_socket);
        reset();
    }
}
