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

package rscminus.testsuite.tests;

import rscminus.common.ChatCipher;
import rscminus.common.ChatFilter;
import rscminus.game.NetworkStream;
import rscminus.game.entity.player.ChatMessage;
import rscminus.testsuite.types.DBTest;

import java.sql.*;
import java.util.Arrays;

/**
 * This unit test requires the database "rscChat." This db contains (1) messages sent by the replay owner in raw form,
 * and (2) messages received by the replay owner in filtered form. This unit test does the following:
 * 1) Reads a raw chat message (1)
 * 2) Runs the message through ChatFilter.filter()
 * 3) Runs the message through ChatCipher.encipher()
 * 4) Copies the enciphered message into a NetworkStream
 * 5) Reads the NetworkStream, decodes the message
 * 6) Compares that decoded message to the matched received message (2)
 *
 * The test ignores censorship, as the replay data was captured with the 235 chat censor,
 * and RSC- uses the 204 chat censor.
 *
 * Finally, a message of max length (NetworkStream buffer size - 3: two for size, one for opcode)
 * is ran through the same process to test edge cases of all the functions.
 */
public class chat extends DBTest {
    private static ChatMessage chatMessage = new ChatMessage();
    private static NetworkStream networkStream = new NetworkStream();

    public boolean run() throws Exception {
        ChatCipher.init();

        Statement receiveChatStatement = newStatement(null);
        Statement sendChatStatement = newStatement(null);
        ResultSet receiveChatResults = receiveChatStatement.executeQuery(
                "SELECT * FROM `rscChat`.`update_players_type_1` ORDER BY `replayIndex` ASC, `timestamp` ASC"
        );
        ResultSet sendChatResults = sendChatStatement.executeQuery(
                "SELECT * FROM `rscChat`.`send_chat` ORDER BY `replayIndex` ASC, `timestamp` ASC"
        );
        while (sendChatResults.next() && receiveChatResults.next()) {
            int sentMessageRI = sendChatResults.getInt(2);
            int receivedMessageRI = receiveChatResults.getInt(2);
            if (sentMessageRI != receivedMessageRI) {
                System.out.println("RI mismatch :".concat(sendChatResults.getInt(1) + " | ".concat(String.valueOf(receiveChatResults.getInt(1)))));
                receiveChatResults.previous();
                continue;
            }
            String sentMessage = sendChatResults.getString(4);
            String receivedMessage = receiveChatResults.getString(4);
            if (sentMessage.length() != receivedMessage.length()) {
                System.out.println("Skipped message: ".concat(sendChatResults.getInt(1) + " | ".concat(String.valueOf(receiveChatResults.getInt(1)))));
                receiveChatResults.previous();
                continue;
            }

            sentMessage = ChatFilter.filter(sentMessage);
            ChatCipher.encipher(sentMessage, chatMessage);
            networkStream.setPosition(0);
            networkStream.writeVariableSize(chatMessage.decipheredLength);
            networkStream.writeArray(chatMessage.messageBuffer,0, chatMessage.encipheredLength & 0xFFFF);
            networkStream.setPosition(0);

            String decoded = ChatCipher.decipher(networkStream);
            if (!compareIgnoreAsterisk(decoded, receivedMessage)) {
                System.out.println(decoded);
                System.out.println(receivedMessage);
                System.out.println(sendChatResults.getInt(1));
                System.out.println(receiveChatResults.getInt(1));
                return false;
            }
        }

        System.out.println("Database test passed. Testing message length...");
        //Test handling a message of max length
        byte[] lengthTest = new byte[chatMessage.messageBuffer.length];
        Arrays.fill(lengthTest, (byte)1);
        networkStream.setPosition(0);
        networkStream.writeVariableSize(lengthTest.length);
        networkStream.writeArray(lengthTest,0, lengthTest.length);
        networkStream.setPosition(0);
        String decoded = ChatCipher.decipher(networkStream);
        decoded = ChatFilter.filter(decoded);
        ChatCipher.encipher(decoded, chatMessage);
        return Arrays.equals(lengthTest, chatMessage.messageBuffer);
    }

    private boolean compareIgnoreAsterisk(String one, String two) {
        if (one.length() != two.length())
            return false;

        for (int i=0; i < one.length(); ++i) {
            if (one.charAt(i) != two.charAt(i)) {
                if (one.charAt(i) != '*' && two.charAt(i) != '*')
                    return false;
            }
        }
        return true;
    }

}
