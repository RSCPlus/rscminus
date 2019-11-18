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

import rscminus.common.Logger;
import rscminus.game.PacketBuilder;
import rscminus.game.Server;

import java.util.LinkedList;

public class ReplayClientExperiments {
    public static LinkedList<ReplayPacket> GenerateSpriteViewer(int beginningTimeStamp) {
        LinkedList<ReplayPacket> newPackets = new LinkedList<ReplayPacket>();
        Logger.Info("@|red Entered Experiments! |@");

        newPackets.add(constructMessagePacket(
                "Test Appending Text Message!", beginningTimeStamp, 0));
        newPackets.add(constructCreatePlayersPacket(210,627,0, beginningTimeStamp + 1));
        newPackets.add(constructMessagePacket(
                "@ran@Test Appending Text Message! 2", beginningTimeStamp + 50, 0));
        newPackets.add(constructCreatePlayersPacket(210,627,0, beginningTimeStamp + 51));
        newPackets.add(constructMessagePacket(
                "@mag@Test Appending Text Message! 3", beginningTimeStamp +  50 * 2, 0));
        newPackets.add(constructCreatePlayersPacket(210,627,0, beginningTimeStamp + 51 * 2));
        newPackets.add(constructMessagePacket(
                "@ora@Test Appending Text Message! 4", beginningTimeStamp + 50 * 3, 0));
        newPackets.add(constructCreatePlayersPacket(210,627,0, beginningTimeStamp + 51 * 3));
        newPackets.add(constructMessagePacket(
                "@gre@Test Appending Text Message! 5", beginningTimeStamp + 50 * 4, 0));
        newPackets.add(constructCreatePlayersPacket(210,627,0, beginningTimeStamp + 51 * 4));

        return newPackets;
    }

    private static ReplayPacket constructMessagePacket(String message, int timeStamp, int messageType) {
        ReplayPacket testMessage = new ReplayPacket();
        testMessage.opcode = PacketBuilder.OPCODE_SEND_MESSAGE;
        byte[] messageBytes = message.getBytes();
        testMessage.data = new byte[messageBytes.length + 4];
        for (int i = 0; i < messageBytes.length; i++) {
            testMessage.data[i + 3] = messageBytes[i];
        }
        testMessage.data[0] = (byte) (messageType & 0xFF);
        testMessage.timestamp = timeStamp;
        return testMessage;
    }

    private static ReplayPacket constructCreatePlayersPacket(int x, int y, int animation, int timeStamp) {
        ReplayPacket testMessage = new ReplayPacket();
        testMessage.opcode = PacketBuilder.OPCODE_CREATE_PLAYERS;
        x &= 2047;
        y &= 8191;
        int coordinate = x << 13 | y;
        testMessage.data = new byte[]{(byte)((coordinate & 0xFF0000) >> 16), (byte)((coordinate & 0xFF00) >> 8), (byte) (coordinate & 0xFF), (byte)0, (byte) 0};
        testMessage.timestamp = timeStamp;
        return testMessage;
    }
}
