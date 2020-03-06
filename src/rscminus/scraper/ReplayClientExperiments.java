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

import org.bouncycastle.util.Pack;
import rscminus.common.Logger;
import rscminus.game.PacketBuilder;
import rscminus.game.Server;
import rscminus.game.constants.Game;
import rscminus.scraper.client.Class11;

import java.util.LinkedList;

public class ReplayClientExperiments {
    private static int tF = (int)((Server.DEFAULT_TICK_RATE / 1000.0) * 50); // (int)32.5 w/650 tick rate
    public static LinkedList<ReplayPacket> GenerateSpriteViewer(int beginningTimeStamp) {
        LinkedList<ReplayPacket> newPackets = new LinkedList<ReplayPacket>();
        Logger.Info("@|red Entered Experiments! |@");
        int ts = beginningTimeStamp;
        int tick = 0;

        newPackets.add(constructMessagePacket(ts + tF * tick,
                "@gre@This replay was artificially produced!", Game.CHAT_QUEST));
        tick += 2;
        newPackets.add(constructMessagePacket(ts + tF * tick,
                "@gre@It will show all the sprites for each slot.",  Game.CHAT_QUEST));

        newPackets.add(constructCreatePlayersPacket(ts + tF * ++tick, 210, 627, 0));

        newPackets.add(constructUpdatePlayersPacketType5(ts + tF * ++tick, "Flying Sno",
                (byte) 4,(byte) 5,(byte) 3,
                (byte) 0, (byte) 0,(byte) 0,(byte) 0,(byte) 0,(byte) 0,(byte) 12, (byte) 172,(byte) 0,
                (byte) 8, (byte) 10,(byte) 3,(byte) 1,(byte) 138,(byte) 1));
        newPackets.add(constructUpdatePlayersPacketType1(
                ts + tF * tick + 1, 2, 2, ">_<< 0aGMmh'oaeme!!!!"));

        tick++;
        for (int i = 0; i < 128; i += 2) {
            newPackets.add(constructUpdatePlayersPacketType5(ts + tF * tick + i, "Flying Sno",
                    (byte) 4, (byte) 2, (byte) 1,
                    (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 12, (byte) 172, (byte) 0,
                    (byte) 8, (byte) 10, (byte) 3, (byte) 1, (byte) 138, (byte) 1));
            newPackets.add(constructUpdatePlayersPacketType5(ts + tF * tick + i + 1, "Flying Sno",
                    (byte) 4, (byte) 5, (byte) 213,
                    (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 12, (byte) 172, (byte) 0,
                    (byte) 8, (byte) 10, (byte) 3, (byte) 1, (byte) 138, (byte) 1));
        }
        tick += 4;

        newPackets.add(constructUpdatePlayersPacketType1(
                ts + tF * tick + 1, 2, 2, "i'm ALIVE!!"));

        // keepalive
        for (int i = 0; i < 4; i++)
            newPackets.add(constructCreatePlayersPacket(ts + tF * ++tick, 210, 627, 0));

        newPackets.add(constructMessagePacket(ts + tF * tick,
                "@gre@...",  Game.CHAT_QUEST));

        // keepalive
        for (int i = 0; i < 4; i++)
            newPackets.add(constructCreatePlayersPacket(ts + tF * ++tick, 210, 627, 0));

        newPackets.add(constructMessagePacket(ts + tF * tick,
                "@gre@Okay, well, welcome back!",  Game.CHAT_QUEST));

        // keepalive
        for (int i = 0; i < 4; i++)
            newPackets.add(constructCreatePlayersPacket(ts + tF * ++tick, 210, 627, 0));

        newPackets.add(constructUpdatePlayersPacketType5(ts + tF * tick, "Flying Sno",
                (byte) 4, (byte) 5, (byte) 3,
                (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 12, (byte) 172, (byte) 0,
                (byte) 8, (byte) 10, (byte) 3, (byte) 1, (byte) 138, (byte) 0));

        newPackets.add(constructUpdatePlayersPacketType1(
                ts + tF * tick + 1, 2, 2, "Thankyou"));

        // keepalive
        for (int i = 0; i < 4; i++)
            newPackets.add(constructCreatePlayersPacket(ts + tF * ++tick, 210, 627, 0));

        newPackets.add(constructMessagePacket(ts + tF * tick,
                "@gre@Let's show some sprites, then:",  Game.CHAT_QUEST));

        newPackets.add(constructCreatePlayersPacket(ts + tF * ++tick, 210, 627, 0));
        newPackets.add(constructCreatePlayersPacket(ts + tF * ++tick, 210, 627, 0));

        newPackets.add(constructUpdatePlayersPacketType1(
                ts + tF * tick + 1, 2, 2, "Observe the many possible states of my body!"));
        for (int i = 0; i < 4; i++)
            newPackets.add(constructCreatePlayersPacket(ts + tF * ++tick, 210, 627, 0));

        for (int i = 0; i < 5; i++) {
            newPackets.add(constructUpdatePlayersPacketType1(
                    ts + tF * tick, 2, 2,
                    String.format("Sprite %d", i)));
            newPackets.add(constructUpdatePlayersPacketType5(ts + tF * tick + 1, "Flying Sno",
                    (byte) i, (byte) i, (byte) i,
                    (byte) i, (byte) i, (byte) i, (byte) i, (byte) i, (byte) i, (byte) i, (byte) i, (byte) i,
                    (byte) i, (byte) i, (byte) i, (byte) i, (byte) i, (byte) i));

            newPackets.add(constructCreatePlayersPacket(ts + tF * ++tick, 210, 627, 0));
        }

        newPackets.add(constructUpdatePlayersPacketType1(
                ts + tF * tick, 2, 2, "If my skin gets any darker, the entire screen will turn black due to an out of bounds error."));
        newPackets.add(constructCreatePlayersPacket(ts + tF * ++tick, 210, 627, 0));
        newPackets.add(constructUpdatePlayersPacketType1(
                ts + tF * ++tick, 2, 2, "Therefore, I will keep my skin colour within bounds, Skin will remain at 4."));
        newPackets.add(constructCreatePlayersPacket(ts + tF * ++tick, 210, 627, 0));
        newPackets.add(constructCreatePlayersPacket(ts + tF * ++tick, 210, 627, 0));
        newPackets.add(constructCreatePlayersPacket(ts + tF * ++tick, 210, 627, 0));

        for (int i = 5; i < 230; i++) {
            newPackets.add(constructUpdatePlayersPacketType1(
                    ts + tF * tick, 2, 2,
                    String.format("Sprite %d", i)));
            newPackets.add(constructUpdatePlayersPacketType5(ts + tF * tick + 1, "Flying Sno",
                    (byte) i, (byte) i, (byte) i,
                    (byte) i, (byte) i, (byte) i, (byte) i, (byte) i, (byte) i, (byte) i, (byte) i, (byte) i,
                    (byte) i, (byte) i, (byte) i, (byte) 4, (byte) i, (byte) i));

            newPackets.add(constructCreatePlayersPacket(ts + tF * ++tick, 210, 627, 0));
        }

        newPackets.add(constructUpdatePlayersPacketType5(ts + tF * ++tick, "Flying Sno",
                (byte) 4, (byte) 5, (byte) 3,
                (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 12, (byte) 172, (byte) 0,
                (byte) 8, (byte) 10, (byte) 3, (byte) 1, (byte) 138, (byte) 0));

        newPackets.add(constructCreatePlayersPacket(ts + tF * ++tick, 210, 627, 0));

        newPackets.add(constructUpdatePlayersPacketType1(
                ts + tF * ++tick, 2, 2, "And that's the entire sprite array!"));

        newPackets.add(constructCreatePlayersPacket(ts + tF * ++tick, 210, 627, 0));
        newPackets.add(constructCreatePlayersPacket(ts + tF * ++tick, 210, 627, 0));

        newPackets.add(constructUpdatePlayersPacketType1(
                ts + tF * ++tick, 2, 2, "Hope you enjoyed and that it was useful."));
        for (int i = 0; i < 4; i++)
            newPackets.add(constructCreatePlayersPacket(ts + tF * ++tick, 210, 627, 0));

        newPackets.add(constructUpdatePlayersPacketType1(
                ts + tF * ++tick, 2, 2, "What's that? You're not satisfied yet?"));
        newPackets.add(constructCreatePlayersPacket(ts + tF * ++tick, 210, 627, 0));
        newPackets.add(constructUpdatePlayersPacketType1(
                ts + tF * ++tick, 2, 2, "Okay, then you have  about 10 ticks  to rewind..."));
        newPackets.add(constructCreatePlayersPacket(ts + tF * ++tick, 210, 627, 0));
        newPackets.add(constructUpdatePlayersPacketType1(
                ts + tF * ++tick, 2, 2, "Then I'm gonna demo breaking the rendering"));

        for (int i = 0; i < 10; i++)
            newPackets.add(constructCreatePlayersPacket(ts + tF * ++tick, 210, 627, 0));

        newPackets.add(constructUpdatePlayersPacketType5(ts + tF * ++tick, "Flying Sno",
                (byte) 4, (byte) 5, (byte) 3,
                (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 12, (byte) 172, (byte) 0,
                (byte) 8, (byte) 10, (byte) 3, (byte) 5, (byte) 138, (byte) 0)); // Skin index is 5, OOB

        newPackets.add(constructUpdatePlayersPacketType1(
                ts + tF * ++tick, 2, 2, "Look Familiar? Unless the bug has been fixed half the landscape shouldn't be rendered right now."));
        newPackets.add(constructCreatePlayersPacket(ts + tF * ++tick, 210, 627, 0));

        /* This doesn't actually work lol?
        newPackets.add(constructUpdatePlayersPacketType1(
                ts + tF * ++tick, 2, 2, "But we can actually recover from this state!"));
        newPackets.add(constructCreatePlayersPacket(ts + tF * ++tick, 210, 627, 0));
        newPackets.add(constructUpdatePlayersPacketType1(
                ts + tF * ++tick, 2, 2, "By passing 255 to the skin index."));
        for (int i = 0; i < 10; i++)
            newPackets.add(constructCreatePlayersPacket(ts + tF * ++tick, 210, 627, 0));
        for (int i = 3; i > 0; i--) {
            newPackets.add(constructMessagePacket(ts + tF * ++tick,
                    String.format("@gre@%d...", i), Game.CHAT_QUEST));
            newPackets.add(constructCreatePlayersPacket(ts + tF * ++tick, 210, 627, 0));
            newPackets.add(constructCreatePlayersPacket(ts + tF * ++tick, 210, 627, 0));
        }
        newPackets.add(constructUpdatePlayersPacketType5(ts + tF * ++tick, "Flying Sno",
                (byte) 4, (byte) 5, (byte) 3,
                (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 12, (byte) 172, (byte) 0,
                (byte) 8, (byte) 10, (byte) 3, (byte) 255, (byte) 138, (byte) 0)); // Skin index is 5, OOB
        ////
        for (int i = 0; i < 10; i++)
            newPackets.add(constructCreatePlayersPacket(ts + tF * ++tick, 210, 627, 0));

        newPackets.add(constructUpdatePlayersPacketType1(
                ts + tF * ++tick, 2, 2, "One more thing, here's the other arrays indexed OOB at 230"));
        newPackets.add(constructCreatePlayersPacket(ts + tF * ++tick, 210, 627, 0));
        newPackets.add(constructCreatePlayersPacket(ts + tF * ++tick, 210, 627, 0));

        newPackets.add(constructUpdatePlayersPacketType5(ts + tF * ++tick, "Flying Sno",
                (byte) 230, (byte) 230, (byte) 230,
                (byte) 230, (byte) 230, (byte) 230, (byte) 230, (byte) 230, (byte) 230, (byte) 230, (byte) 230, (byte) 230,
                (byte) 230, (byte) 230, (byte) 230, (byte) 4, (byte) 230, (byte) 230)); // everything but skin OOB
        */
        for (int i = 0; i < 10; i++)
            newPackets.add(constructCreatePlayersPacket(ts + tF * ++tick, 210, 627, 0));

        newPackets.add(constructMessagePacket(ts + tF * ++tick,
                "@ran@That's all folks!", Game.CHAT_QUEST));
        // keepalive
        for (int i = 0; i < 4; i++)
            newPackets.add(constructCreatePlayersPacket(ts + tF * ++tick, 210, 627, 0));


        return newPackets;
    }

    private static ReplayPacket constructMessagePacket(int timeStamp, String message, int messageType) {
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

    private static ReplayPacket constructCreatePlayersPacket(int timeStamp, int x, int y, int animation) {
        ReplayPacket testMessage = new ReplayPacket();
        testMessage.opcode = PacketBuilder.OPCODE_CREATE_PLAYERS;
        x &= 2047;
        y &= 8191;
        int coordinate = x << 13 | y;
        animation &= 0xF;
        animation <<= 4;
        testMessage.data = new byte[]{(byte)((coordinate & 0xFF0000) >> 16), (byte)((coordinate & 0xFF00) >> 8), (byte) (coordinate & 0xFF), (byte)animation, (byte) 0};
        testMessage.timestamp = timeStamp;
        return testMessage;
    }

    private static ReplayPacket constructUpdatePlayersPacketType1(int timeStamp, int PID, int modStatus, String message) {
        byte[] scrambledMessage = new byte[1000];
        Class11 stringEncryptor = new Class11(new byte[]{(byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 21, (byte) 22, (byte) 22, (byte) 20, (byte) 22, (byte) 22, (byte) 22, (byte) 21, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 3, (byte) 8, (byte) 22, (byte) 16, (byte) 22, (byte) 16, (byte) 17, (byte) 7, (byte) 13, (byte) 13, (byte) 13, (byte) 16, (byte) 7, (byte) 10, (byte) 6, (byte) 16, (byte) 10, (byte) 11, (byte) 12, (byte) 12, (byte) 12, (byte) 12, (byte) 13, (byte) 13, (byte) 14, (byte) 14, (byte) 11, (byte) 14, (byte) 19, (byte) 15, (byte) 17, (byte) 8, (byte) 11, (byte) 9, (byte) 10, (byte) 10, (byte) 10, (byte) 10, (byte) 11, (byte) 10, (byte) 9, (byte) 7, (byte) 12, (byte) 11, (byte) 10, (byte) 10, (byte) 9, (byte) 10, (byte) 10, (byte) 12, (byte) 10, (byte) 9, (byte) 8, (byte) 12, (byte) 12, (byte) 9, (byte) 14, (byte) 8, (byte) 12, (byte) 17, (byte) 16, (byte) 17, (byte) 22, (byte) 13, (byte) 21, (byte) 4, (byte) 7, (byte) 6, (byte) 5, (byte) 3, (byte) 6, (byte) 6, (byte) 5, (byte) 4, (byte) 10, (byte) 7, (byte) 5, (byte) 6, (byte) 4, (byte) 4, (byte) 6, (byte) 10, (byte) 5, (byte) 4, (byte) 4, (byte) 5, (byte) 7, (byte) 6, (byte) 10, (byte) 6, (byte) 10, (byte) 22, (byte) 19, (byte) 22, (byte) 14, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 21, (byte) 22, (byte) 21, (byte) 22, (byte) 22, (byte) 22, (byte) 21, (byte) 22, (byte) 22});
        int byteLength = stringEncryptor.method161(scrambledMessage, message);

        ReplayPacket np = new ReplayPacket();

        np.opcode = PacketBuilder.OPCODE_UPDATE_PLAYERS;
        np.timestamp = timeStamp;
        np.data = new byte[byteLength + 6];

        np.data[1] = 1; // Player Count
        np.data[2] = (byte)((PID & 0xFF00) >> 8);
        np.data[3] = (byte)(PID & 0xFF);
        np.data[4] = 1; // Update Type
        np.data[5] = (byte) (modStatus & 0xFF);
        for (int i = 0; i < byteLength; i++) {
            np.data[i + 6] = scrambledMessage[i];
        }

        return np;
    }
    private static ReplayPacket constructUpdatePlayersPacketType5(int timeStamp, String usernameString,
                                                                  byte hair, byte shirt, byte pants, byte shield,
                                                                  byte weapon, byte head, byte body, byte legs,
                                                                  byte gloves, byte boots, byte amulet, byte cape,
                                                                  byte hairColour,  byte topColour, byte pantsColour,
                                                                  byte skinColour, byte combatLevel, byte skull) {
        ReplayPacket np = new ReplayPacket();
        np.opcode = PacketBuilder.OPCODE_UPDATE_PLAYERS;
        np.timestamp = timeStamp;
        byte[] username = usernameString.getBytes();
        np.data = new byte[30 + username.length * 2];

        np.data[0] = 0; np.data[1] = 1; // Player Count: 1
        np.data[2] = 0; np.data[3] = 2; // PID = 2
        np.data[4] = 5; // Update Type: 5
        //np.data[5] = 0x0c; np.data[6] = (byte)0xce; // Unused

        for (int i = 0; i < username.length; i++) {
            np.data[8 +  i] = username[i];
            np.data[10 + username.length + i] = username[i];
        }
        int offset  = (username.length + 2) * 2 + 6;

        np.data[offset + 1] = 12; //Equipment Count
        np.data[offset + 2] = hair;
        np.data[offset + 3] = shirt;
        np.data[offset + 4] = pants;
        np.data[offset + 5] = shield;
        np.data[offset + 6] = weapon;
        np.data[offset + 7] = head;
        np.data[offset + 8] = body;
        np.data[offset + 9] = legs;
        np.data[offset + 10] = gloves;
        np.data[offset + 11] = boots;
        np.data[offset + 12] = amulet;
        np.data[offset + 13] = cape;

        np.data[offset + 14] = hairColour;
        np.data[offset + 15] = topColour;
        np.data[offset + 16] = pantsColour;
        np.data[offset + 17] = skinColour;
        np.data[offset + 18] = combatLevel;
        np.data[offset + 19] = skull;

        return np;

    }
}
