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

package rscminus.common;

import rscminus.game.NetworkStream;

import java.util.HashMap;

public class StringEncryption {

    //Unicode characters that are mapped to specific byte values
    //The array is for byte->char, the dict is for char->byte
    private final static char[] specialCharacters = new char[] { '\u20ac', '?', '\u201a', '\u0192', '\u201e', '\u2026', '\u2020', '\u2021', '\u02c6',
            '\u2030', '\u0160', '\u2039', '\u0152', '?', '\u017d', '?', '?', '\u2018', '\u2019', '\u201c',
            '\u201d', '\u2022', '\u2013', '\u2014', '\u02dc', '\u2122', '\u0161', '\u203a', '\u0153', '?',
            '\u017e', '\u0178' };
    private final static HashMap<Character, Byte> specialCharacterMap = new HashMap<>();

    //Used to build the cipher blocks. Copied from the client.
    private final static byte[] init = new byte[] { 22, 22, 22, 22, 22, 22, 21, 22, 22, 20, 22, 22, 22, 21, 22, 22,
            22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 3, 8, 22, 16, 22, 16, 17, 7, 13, 13, 13, 16,
            7, 10, 6, 16, 10, 11, 12, 12, 12, 12, 13, 13, 14, 14, 11, 14, 19, 15, 17, 8, 11, 9, 10, 10, 10, 10, 11, 10,
            9, 7, 12, 11, 10, 10, 9, 10, 10, 12, 10, 9, 8, 12, 12, 9, 14, 8, 12, 17, 16, 17, 22, 13, 21, 4, 7, 6, 5, 3,
            6, 6, 5, 4, 10, 7, 5, 6, 4, 4, 6, 10, 5, 4, 4, 5, 7, 6, 10, 6, 10, 22, 19, 22, 14, 22, 22, 22, 22, 22, 22,
            22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22,
            22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22,
            22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22,
            22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22,
            22, 22, 22, 22, 22, 22, 21, 22, 21, 22, 22, 22, 21, 22, 22 };

    //Cipher blocks
    private final static int[] a = new int[init.length];
    private static int[] b = new int[8];

    //Used for both enciphering and deciphering chat
    private static final byte[] chatBuffer = new byte[256];

    private final static StringBuilder messageBuilder = new StringBuilder();

    public static void init() {
        //Initialize the special character map
        for (int i=0; i < specialCharacters.length; ++i)
            specialCharacterMap.put(specialCharacters[i], (byte)(i - 128));

        //Initialize arrays for ciphering
        final int[] c = new int[33];
        int bIndexTemp = 0;
        for (int initPos = 0; initPos < init.length; ++initPos)
        {
            final int initValue = init[initPos];
            final int cBitSelector = 1 << (32 - initValue);
            final int cValue = c[initValue];
            a[initPos] = cValue;
            int cValueBit;
            if ((cValue & cBitSelector) == 0)
            {
                cValueBit = cValue | cBitSelector;
                for (int initValueCounter = initValue - 1; initValueCounter > 0; --initValueCounter)
                {
                    final int cValueTemp = c[initValueCounter];
                    if (cValue != cValueTemp)
                        break;
                    final int cValueTempBitSelector = 1 << (32 - initValueCounter);
                    if ((cValueTemp & cValueTempBitSelector) == 0)
                    {
                        c[initValueCounter] = cValueTempBitSelector | cValueTemp;
                    }
                    else
                    {
                        c[initValueCounter] = c[initValueCounter - 1];
                        break;
                    }
                }
            }
            else
            {
                cValueBit = c[initValue + -1];
            }
            c[initValue] = cValueBit;
            for (int initValueCounter = initValue + 1; initValueCounter <= 32; ++initValueCounter)
            {
                if (cValue == c[initValueCounter])
                {
                    c[initValueCounter] = cValueBit;
                }
            }
            int bIndex = 0;
            for (int initValueCounter = 0; initValueCounter < initValue; ++initValueCounter)
            {
                int cBitSelector2 = 0x80000000 >>> initValueCounter;
                if ((cValue & cBitSelector2) == 0)
                {
                    bIndex++;
                }
                else
                {
                    if (b[bIndex] == 0)
                        b[bIndex] = bIndexTemp;

                    bIndex = b[bIndex];
                }
                if (b.length <= bIndex)
                {
                    final int[] bResized = new int[b.length * 2];
                    System.arraycopy(b, 0, bResized, 0, b.length);
                    b = bResized;
                }
            }
            b[bIndex] = ~initPos;
            if (bIndex >= bIndexTemp)
                bIndexTemp = bIndex + 1;
        }
    }

    public static void encipher(byte[] outputBuffer, String message) {
        convertMessageToBytes(message);
        int encipheredByte = 0;
        int outputBitOffset = 16;
        for (int messageIndex = 0; message.length() > messageIndex; ++messageIndex)
        {
            final int messageCharacter = chatBuffer[messageIndex] & 0xff;
            final int aValue = a[messageCharacter];
            final int initValue = init[messageCharacter];

            int outputByteOffset = outputBitOffset >> 3;
            int temp = 0x7 & outputBitOffset;
            encipheredByte &= -temp >> 31;
            final int outputByteOffset2 = outputByteOffset + ((temp + initValue - 1) >> 3);
            outputBitOffset += initValue;
            temp += 24;
            encipheredByte |= (aValue >>> temp);
            outputBuffer[outputByteOffset] = (byte) (encipheredByte);
            if (outputByteOffset2 > outputByteOffset)
            {
                outputByteOffset++;
                temp -= 8;
                outputBuffer[outputByteOffset] = (byte) (encipheredByte = aValue >>> temp);
                if (outputByteOffset < outputByteOffset2)
                {
                    outputByteOffset++;
                    temp -= 8;
                    outputBuffer[outputByteOffset] = (byte) (encipheredByte = aValue >>> temp);
                    if (outputByteOffset2 > outputByteOffset)
                    {
                        outputByteOffset++;
                        temp -= 8;
                        outputBuffer[outputByteOffset] = (byte) (encipheredByte = aValue >>> temp);
                        if (outputByteOffset2 > outputByteOffset)
                        {
                            temp -= 8;
                            outputByteOffset++;
                            outputBuffer[outputByteOffset] = (byte) (encipheredByte = aValue << -temp);
                        }
                    }
                }
            }
        }
        outputBuffer[0] = (byte)(((outputBitOffset + 7) >> 3) + -1);
        outputBuffer[1] = (byte)message.length();
    }

    public static String decipher(final NetworkStream m_packetStream, final int messageLength) {
        int bufferIndex = 0;
        int temp = 0;
        int bValue;

        while (bufferIndex < messageLength)
        {
            final byte encipheredByte = m_packetStream.readByte();
            temp = encipheredByte < 0 ? b[temp] : temp + 1;

            if (0 > (bValue = b[temp]))
            {
                chatBuffer[bufferIndex++] = (byte) (~bValue);
                if (bufferIndex >= messageLength)
                    break;

                temp = 0;
            }

            int andVal = 0x40;
            while (andVal > 0) {
                temp = (encipheredByte & andVal) == 0 ? temp + 1 : b[temp];
                if ((bValue = b[temp]) < 0) {
                    chatBuffer[bufferIndex++] = (byte) (~bValue);
                    if (bufferIndex >= messageLength)
                        break;

                    temp = 0;
                }
                andVal >>= 1;
            }
        }

        return buildMessage(messageLength);
    }

    public static void convertMessageToBytes(CharSequence charSequence) {
        for (int messageIndex = 0; messageIndex < charSequence.length() ; ++messageIndex)
        {
            final char c = charSequence.charAt(messageIndex);
            if ((('\0' >= c) || ('\u0080' <= c)) && (('\u00a0' > c) || ('\u00ff' < c)))
            {
                if (specialCharacterMap.containsKey(c))
                    chatBuffer[messageIndex] = specialCharacterMap.get(c);
                else
                    chatBuffer[messageIndex] = 63;
            } else
                chatBuffer[messageIndex] = (byte)c;
        }
    }

    public static String buildMessage(int messageLength) {
        messageBuilder.setLength(0);

        for (int bufferIndex = 0; bufferIndex < messageLength; ++bufferIndex)
        {
            int bufferValue = chatBuffer[bufferIndex] & 0xFF;
            if (bufferValue != 0)
            {
                if (bufferValue >= 128 && bufferValue < 160)
                    bufferValue = specialCharacters[bufferValue - 128];

                messageBuilder.append((char)bufferValue);
            }
        }

        return messageBuilder.toString();
    }
}
