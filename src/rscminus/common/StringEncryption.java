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

    private final static char[] specialCharacters = new char[] { '\u20ac', '\0', '\u201a', '\u0192', '\u201e', '\u2026', '\u2020', '\u2021', '\u02c6',
            '\u2030', '\u0160', '\u2039', '\u0152', '\0', '\u017d', '\0', '\0', '\u2018', '\u2019', '\u201c',
            '\u201d', '\u2022', '\u2013', '\u2014', '\u02dc', '\u2122', '\u0161', '\u203a', '\u0153', '\0',
            '\u017e', '\u0178' };

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

    private final static int[] a = new int[init.length];
    private static int[] b = new int[8];

    private final static StringBuilder messageBuilder = new StringBuilder();

    public static void init() {
        //Initialize arrays for ciphering
        final int[] c = new int[33];
        int i_21_ = 0;
        for (int initPos = 0; initPos < init.length; ++initPos)
        {
            final int initValue = init[initPos];
            if (initValue != 0)
            {
                final int initBitSelector = 1 << (32 - initValue);
                final int cValue = c[initValue];
                a[initPos] = cValue;
                int cValueBit;
                if ((cValue & initBitSelector) == 0)
                {
                    cValueBit = cValue | initBitSelector;
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
                int i_31_ = 0;
                for (int initValueCounter = 0; initValueCounter < initValue; ++initValueCounter)
                {
                    int i_33_ = 0x80000000 >>> initValueCounter;
                    if ((cValue & i_33_) == 0)
                    {
                        i_31_++;
                    }
                    else
                    {
                        if (b[i_31_] == 0)
                        {
                            b[i_31_] = i_21_;
                        }
                        i_31_ = b[i_31_];
                    }
                    if (b.length <= i_31_)
                    {
                        final int[] is_34_ = new int[b.length * 2];
                        for (int i_35_ = 0; i_35_ < b.length; i_35_++)
                        {
                            is_34_[i_35_] = b[i_35_];
                        }
                        b = is_34_;
                    }
                }
                b[i_31_] = ~initPos;
                if (i_31_ >= i_21_)
                {
                    i_21_ = i_31_ + 1;
                }
            }
        }
    }

    public static int encipher() {
        return 0;
    }

    public static void decipher(final byte[] outputBuffer, final NetworkStream m_packetStream, final int messageLength) {
        int i_1_ = 0;
        int i_4_ = 0;
        for (;;)
        {
            final byte i_6_ = m_packetStream.readByte();
            if (0 > i_6_)
            {
                i_4_ = b[i_4_];
            }
            else
            {
                i_4_++;
            }
            int i_7_;
            if (0 > (i_7_ = b[i_4_]))
            {
                outputBuffer[i_1_++] = (byte) (~i_7_);
                if (i_1_ >= messageLength)
                {
                    break;
                }
                i_4_ = 0;
            }
            if ((0x40 & i_6_) != 0)
            {
                i_4_ = b[i_4_];
            }
            else
            {
                i_4_++;
            }
            if ((i_7_ = b[i_4_]) < 0)
            {
                outputBuffer[i_1_++] = (byte) (~i_7_);
                if (i_1_ >= messageLength)
                {
                    break;
                }
                i_4_ = 0;
            }
            if ((0x20 & i_6_) != 0)
            {
                i_4_ = b[i_4_];
            }
            else
            {
                i_4_++;
            }
            if ((i_7_ = b[i_4_]) < 0)
            {
                outputBuffer[i_1_++] = (byte) (~i_7_);
                if (i_1_ >= messageLength)
                {
                    break;
                }
                i_4_ = 0;
            }
            if ((0x10 & i_6_) == 0)
            {
                i_4_++;
            }
            else
            {
                i_4_ = b[i_4_];
            }
            if (0 > (i_7_ = b[i_4_]))
            {
                outputBuffer[i_1_++] = (byte) (~i_7_);
                if (i_1_ >= messageLength)
                {
                    break;
                }
                i_4_ = 0;
            }
            if ((0x8 & i_6_) == 0)
            {
                i_4_++;
            }
            else
            {
                i_4_ = b[i_4_];
            }
            if ((i_7_ = b[i_4_]) < 0)
            {
                outputBuffer[i_1_++] = (byte) (~i_7_);
                if (i_1_ >= messageLength)
                {
                    break;
                }
                i_4_ = 0;
            }
            if ((0x4 & i_6_) != 0)
            {
                i_4_ = b[i_4_];
            }
            else
            {
                i_4_++;
            }
            if ((i_7_ = b[i_4_]) < 0)
            {
                outputBuffer[i_1_++] = (byte) (~i_7_);
                if (i_1_ >= messageLength)
                {
                    break;
                }
                i_4_ = 0;
            }
            if ((i_6_ & 0x2) == 0)
            {
                i_4_++;
            }
            else
            {
                i_4_ = b[i_4_];
            }
            if (0 > (i_7_ = b[i_4_]))
            {
                outputBuffer[i_1_++] = (byte) (~i_7_);
                if (messageLength <= i_1_)
                {
                    break;
                }
                i_4_ = 0;
            }
            if ((0x1 & i_6_) == 0)
            {
                i_4_++;
            }
            else
            {
                i_4_ = b[i_4_];
            }
            if ((i_7_ = b[i_4_]) < 0)
            {
                outputBuffer[i_1_++] = (byte) (~i_7_);
                if (i_1_ >= messageLength)
                {
                    break;
                }
                i_4_ = 0;
            }
        }
    }

    public static String buildString(final byte[] inputBuffer, int messageLength) {
        //Clear the stringbuilder
        messageBuilder.setLength(0);

        int count = 0;
        for (int i_14_ = 0; i_14_ < messageLength; i_14_++)
        {
            int i_15_ = 0xff & inputBuffer[i_14_];
            if (i_15_ != 0)
            {
                if ((i_15_ >= 128) && (i_15_ < 160))
                {
                    int i_16_ = specialCharacters[i_15_ - 128];
                    if (i_16_ == 0)
                    {
                        i_16_ = 63;
                    }
                    i_15_ = i_16_;
                }
                messageBuilder.append((char) i_15_);
            }
        }
        return messageBuilder.toString();
    }
}
