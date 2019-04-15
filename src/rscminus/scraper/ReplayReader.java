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

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class ReplayReader {
    private byte[] m_data;
    private LinkedHashMap<Integer,Integer> m_timestamps = new LinkedHashMap<Integer,Integer>();
    LinkedList<Integer> m_disconnectOffsets = new LinkedList<Integer>();

    // Reader state
    private boolean m_loggedIn;
    private int m_position;
    private LinkedList<ReplayKeyPair> m_keys;
    private int m_keyIndex;
    private ISAACCipher isaac = new ISAACCipher();

    public static final int TIMESTAMP_EOF = -1;

    public void open(File f, ReplayVersion replayVersion, LinkedList<ReplayKeyPair> keys) throws IOException {
        // Allocate space for data without replay headers
        m_data = new byte[calculateSize(f)];

        // Read replay data
        DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(f))));
        int timestamp = 0;
        int lastTimestamp = timestamp;
        int offset = 0;
        int lastOffset = offset;
        while ((timestamp = in.readInt()) != TIMESTAMP_EOF) {
            m_timestamps.put(offset, timestamp);

            // Handle v0 disconnect
            if (replayVersion.version == 0 && (timestamp - lastTimestamp) > 400)
                m_disconnectOffsets.add(offset);

            lastOffset = offset;
            int length = in.readInt();
            if (length > 0) {
                in.read(m_data, offset, length);
                offset += length;
            }
            if (length == -1 && replayVersion.version != 0) {
                m_disconnectOffsets.add(offset);
            }
            lastTimestamp = timestamp;
        }

        in.close();

        m_loggedIn = false;
        m_position = 0;
        m_keys = keys;
        m_keyIndex = -1;
    }

    public LinkedList<Integer> getDisconnectTimestamps() {
        LinkedList<Integer> timestamps = new LinkedList<Integer>();
        for (int offset : m_disconnectOffsets)
            timestamps.add(findTimestamp(offset));
        return new LinkedList<Integer>(new LinkedHashSet<Integer>(timestamps));
    }

    public ReplayPacket readPacket(boolean outgoing, LinkedList<Integer> timestamps) {
        if (isEOF())
            return null;

        // Handle disconnect
        if (m_disconnectOffsets.contains(m_position)) {
            m_loggedIn = false;
        }

        // Handle timestamp disconnect
        if (timestamps != null && timestamps.size() > 0) {
            int timestamp = findTimestamp(m_position);
            int disconnectTimestamp = timestamps.get(0);
            if (timestamp >= disconnectTimestamp) {
                timestamps.remove(0);
                m_loggedIn = false;
            }
        }

        ReplayPacket replayPacket = new ReplayPacket();
        if (!m_loggedIn) {
            if (outgoing) {
                int length = readPacketLength();
                if (length > 1) {
                    int dataLength = length - 1;
                    replayPacket.data = new byte[dataLength];
                    if (length < 160) {
                        replayPacket.data[dataLength - 1] = readByte();
                        replayPacket.opcode = readUnsignedByte();
                        if (dataLength > 1)
                            read(replayPacket.data, 0, dataLength - 1);
                    } else {
                        replayPacket.opcode = readUnsignedByte();
                        read(replayPacket.data, 0, dataLength);
                    }
                } else {
                    replayPacket.data = null;
                    replayPacket.opcode = readUnsignedByte();
                }
                replayPacket.timestamp = findTimestamp(m_position - 1);

                if (replayPacket.opcode != 0) {
                    System.out.println("ERROR: Invalid login packet: " + replayPacket.opcode);
                    return null;
                }

                // Set isaac keys
                isaac.reset();
                isaac.setKeys(m_keys.get(++m_keyIndex).keys);

                replayPacket.opcode = ReplayEditor.VIRTUAL_OPCODE_CONNECT;

                m_loggedIn = true;
            } else {
                // Handle login response
                int loginResponse = readUnsignedByte();
                if ((loginResponse & 64) != 0)
                    m_loggedIn = true;
                else
                    return null;

                // Set isaac keys
                isaac.reset();
                isaac.setKeys(m_keys.get(++m_keyIndex).keys);

                // Create virtual connect packet
                replayPacket.opcode = ReplayEditor.VIRTUAL_OPCODE_CONNECT;
                replayPacket.data = new byte[1];
                replayPacket.data[0] = (byte) loginResponse;

                // Set timestamp
                replayPacket.timestamp = findTimestamp(m_position - 1);
            }
        } else {
            int length = readPacketLength();
            if (length > 1) {
                int dataLength = length - 1;
                replayPacket.data = new byte[dataLength];
                if (length < 160) {
                    replayPacket.data[dataLength - 1] = readByte();
                    replayPacket.opcode = readUnsignedByte();
                    if (dataLength > 1)
                        read(replayPacket.data, 0, dataLength - 1);
                } else {
                    replayPacket.opcode = readUnsignedByte();
                    read(replayPacket.data, 0, dataLength);
                }
            } else {
                replayPacket.data = null;
                replayPacket.opcode = readUnsignedByte();
            }
            replayPacket.opcode = (replayPacket.opcode - isaac.getNextValue()) & 0xFF;
            replayPacket.timestamp = findTimestamp(m_position - 1);
        }
        return replayPacket;
    }

    private int findTimestamp(int offset) {
        int timestamp = 0;
        Iterator<Map.Entry<Integer, Integer>> iterator = m_timestamps.entrySet().iterator();
        while(iterator.hasNext()) {
            Map.Entry<Integer, Integer> entry = iterator.next();
            if (entry.getKey() <= offset) {
                timestamp = entry.getValue();
                continue;
            } else if (entry.getKey() > offset) {
                break;
            }
        }
        return timestamp;
    }

    private boolean isEOF() {
        return (m_position == m_data.length);
    }

    private void read(byte[] data, int offset, int length) {
        System.arraycopy(m_data, m_position, data, offset, length);
        m_position += length;
    }

    private byte readByte() {
        return m_data[m_position++];
    }

    private int readUnsignedByte() {
        return readByte() & 0xFF;
    }

    public int readPacketLength() {
        int length = readUnsignedByte();
        if (length >= 160)
            length = 256 * length - (40960 - readUnsignedByte());
        return length;
    }

    private int calculateSize(File f) throws IOException {
        DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(f))));
        int timestamp = 0;
        int size = 0;
        while ((timestamp = in.readInt()) != TIMESTAMP_EOF) {
            int length = in.readInt();
            if (length > 0) {
                size += length;
                in.skipBytes(length);
            }
        }
        in.close();
        return size;
    }
}
