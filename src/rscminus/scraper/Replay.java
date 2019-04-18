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

import rscminus.common.ISAACCipher;
import rscminus.common.MathUtil;
import rscminus.game.PacketBuilder;

import java.io.*;
import java.util.LinkedList;
import java.util.zip.GZIPInputStream;

public class Replay {
    private byte m_data[];
    private int m_size;
    private int m_position;
    private int m_bitmaskPosition;
    private int m_version;
    private boolean m_valid;

    public static final int TIMESTAMP_EOF = -1;

    public Replay() {
        m_position = 0;
    }

    public void startBitmask() {
        m_bitmaskPosition = m_position << 3;
    }

    public void endBitmask() {
        m_position = (m_bitmaskPosition + 7) >> 3;
    }

    public int tell() {
        return m_position;
    }

    public int readBitmask(int size) {
        int start = m_bitmaskPosition >> 3;
        int bitEnd = m_bitmaskPosition + size;
        int byteSize = ((bitEnd + 7) >> 3) - start;
        int offset = ((start + byteSize) << 3) - bitEnd;
        int bitmask = MathUtil.getBitmask(size);

        int ret = 0;
        for (int i = 0; i < byteSize; i++)
            ret |= (m_data[start + (byteSize - i - 1)] & 0xFF) << (i << 3);

        m_bitmaskPosition += size;
        return (ret >> offset) & bitmask;
    }

    public byte readByte() {
        return m_data[m_position++];
    }

    public int readUnsignedByte() {
        return m_data[m_position++] & 0xFF;
    }

    public int readUnsignedShort() {
        return (readUnsignedByte() << 8) | readUnsignedByte();
    }

    public int readPacketLength() {
        int length = readUnsignedByte();
        if (length >= 160)
            length = 256 * length - (40960 - readUnsignedByte());
        return length;
    }

    public String readString() {
        int length = 0;
        while(m_data[m_position + length] != '\0')
            length++;
        String ret;
        ret = new String(m_data, m_position, length);
        m_position += length + 1;
        return ret;
    }

    public void skip(int amount) {
        m_position += amount;
    }

    public void flip() {
        m_position = 0;
    }

    public boolean isEOF() {
        return (m_position >= m_size);
    }

    public boolean isValid() {
        return m_valid;
    }

    public int available() {
        return m_size - m_position;
    }

    public void load(String path) {
        m_valid = true;
        int offset = 0;
        LinkedList<Integer> disconnectOffsets = new LinkedList<Integer>();
        try {
            if (new File(path + "/keys.bin").length() == 0) {
                m_valid = false;
                return;
            }

            m_size = calculateSize(path);
            m_data = new byte[m_size];
            DataInputStream version = new DataInputStream(new BufferedInputStream(new FileInputStream(new File(path + "/version.bin"))));
            m_version = version.readInt();
            version.close();

            //System.out.println("version:" + m_version);

            DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(new File(path + "/in.bin.gz")))));
            int timestamp = 0;
            int timestampPrev = timestamp;
            while ((timestamp = in.readInt()) != TIMESTAMP_EOF) {
                if (m_version == 0 && timestamp - timestampPrev > 400)
                    disconnectOffsets.add(offset);

                int length = in.readInt();
                if (length != -1) {
                    in.read(m_data, offset, length);
                    offset += length;
                } else {
                    disconnectOffsets.add(offset);
                }
                timestampPrev = timestamp;
            }
            in.close();
        } catch (Exception e) {
            m_size = offset;
        }

        try {
            removeEncryption(path, disconnectOffsets);
        } catch (Exception e) {
            m_size = m_position + 1;
        }

        flip();
    }

    private void removeEncryption(String path, LinkedList<Integer> disconnectOffsets) throws IOException {
        ISAACCipher isaacCipher = new ISAACCipher();
        isaacCipher.reset();

        DataInputStream in = new DataInputStream(new FileInputStream(new File(path + "/keys.bin")));

        boolean loggedIn = false;
        boolean checkIsaac = false;
        while (m_position < m_size) {
            if (disconnectOffsets.contains(m_position))
                loggedIn = false;

            if (loggedIn) {
                int length = readPacketLength();
                if (length < 160 && length > 1) {
                    int copySize = length - 1;
                    byte copy[] = new byte[copySize];
                    byte swap = m_data[m_position];
                    System.arraycopy(m_data, m_position + 1, copy, 0, copySize);
                    System.arraycopy(copy, 0, m_data, m_position, copySize);
                    m_data[m_position + length - 1] = swap;
                }
                int opcodeOffset = m_position;
                int opcode = (readUnsignedByte() - isaacCipher.getNextValue()) & 0xFF;

                if (checkIsaac && opcode != PacketBuilder.OPCODE_PRIVACY_SETTINGS)
                    break;

                m_data[opcodeOffset] = (byte)opcode;
                skip(length - 1);
                checkIsaac = false;
            } else {
                int keys[] = { in.readInt(), in.readInt(), in.readInt(), in.readInt() };
                isaacCipher.reset();
                isaacCipher.setKeys(keys);
                int responseOffset = m_position;
                int loginResponse = readUnsignedByte();
                if ((loginResponse & 64) != 0)
                    loggedIn = true;
                else
                    break;
                m_data[responseOffset] = 0x00;
                checkIsaac = true;
                //System.out.println("loginresponse:" + loginResponse);
            }
        }
        in.close();

        if (m_position < m_size)
            m_size = m_position + 1;
    }

    private int calculateSize(String path) throws IOException {
        int size = 0;
        DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(new File(path + "/in.bin.gz")))));
        int timestamp = 0;
        while ((timestamp = in.readInt()) != TIMESTAMP_EOF) {
            int length = in.readInt();
            if (length != -1) {
                in.skipBytes(length);
                size += length;
            }
        }
        in.close();
        return size;
    }

    //should be the same as calculateSize but this one is from RSC+ so it's safer to use it
    public static int getReplayEnding(File replay) {
        int timestamp_ret = 0;

        try {
            DataInputStream fileInput =
                    new DataInputStream(
                            new BufferedInputStream(new GZIPInputStream(new FileInputStream(replay))));
            for (; ; ) {
                int timestamp_input = fileInput.readInt();

                // EOF
                if (timestamp_input == -1) break;

                // Skip data, we need to find the last timestamp
                int length = fileInput.readInt();
                if (length > 0) {
                    int skipped = fileInput.skipBytes(length);

                    if (skipped != length) break;
                }

                timestamp_ret = timestamp_input;
            }
            fileInput.close();
        } catch (Exception e) {
            // e.printStackTrace();
        }

        return timestamp_ret;
    }

    public void dump(String fname) {
        File f = new File(fname);
        try {
            DataOutputStream out = new DataOutputStream(new FileOutputStream(f));
            out.write(m_data, 0, m_size);
            out.close();
        } catch (Exception e) {
        }
    }
}
