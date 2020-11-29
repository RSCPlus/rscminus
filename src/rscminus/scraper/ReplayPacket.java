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
import rscminus.common.MathUtil;
import rscminus.scraper.client.Class11;

import java.math.BigInteger;
import java.util.Comparator;

public class ReplayPacket {
    public int timestamp;
    public int opcode;
    public byte[] data;

    private static Class11 stringDecrypter = new Class11(new byte[]{(byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 21, (byte) 22, (byte) 22, (byte) 20, (byte) 22, (byte) 22, (byte) 22, (byte) 21, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 3, (byte) 8, (byte) 22, (byte) 16, (byte) 22, (byte) 16, (byte) 17, (byte) 7, (byte) 13, (byte) 13, (byte) 13, (byte) 16, (byte) 7, (byte) 10, (byte) 6, (byte) 16, (byte) 10, (byte) 11, (byte) 12, (byte) 12, (byte) 12, (byte) 12, (byte) 13, (byte) 13, (byte) 14, (byte) 14, (byte) 11, (byte) 14, (byte) 19, (byte) 15, (byte) 17, (byte) 8, (byte) 11, (byte) 9, (byte) 10, (byte) 10, (byte) 10, (byte) 10, (byte) 11, (byte) 10, (byte) 9, (byte) 7, (byte) 12, (byte) 11, (byte) 10, (byte) 10, (byte) 9, (byte) 10, (byte) 10, (byte) 12, (byte) 10, (byte) 9, (byte) 8, (byte) 12, (byte) 12, (byte) 9, (byte) 14, (byte) 8, (byte) 12, (byte) 17, (byte) 16, (byte) 17, (byte) 22, (byte) 13, (byte) 21, (byte) 4, (byte) 7, (byte) 6, (byte) 5, (byte) 3, (byte) 6, (byte) 6, (byte) 5, (byte) 4, (byte) 10, (byte) 7, (byte) 5, (byte) 6, (byte) 4, (byte) 4, (byte) 6, (byte) 10, (byte) 5, (byte) 4, (byte) 4, (byte) 5, (byte) 7, (byte) 6, (byte) 10, (byte) 6, (byte) 10, (byte) 22, (byte) 19, (byte) 22, (byte) 14, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 22, (byte) 21, (byte) 22, (byte) 21, (byte) 22, (byte) 22, (byte) 22, (byte) 21, (byte) 22, (byte) 22});
    private int m_position;
    private int m_bitmaskPosition;

    ReplayPacket() {
        m_position = 0;
        m_bitmaskPosition = 0;
    }

    public void startBitmask() {
        m_bitmaskPosition = m_position << 3;
    }

    public void endBitmask() {
        m_position = (m_bitmaskPosition + 7) >> 3;
    }

    public int readBitmask(int size) {
        int start = m_bitmaskPosition >> 3;
        int bitEnd = m_bitmaskPosition + size;
        int byteSize = ((bitEnd + 7) >> 3) - start;
        int offset = ((start + byteSize) << 3) - bitEnd;
        int bitmask = MathUtil.getBitmask(size);

        int ret = 0;
        for (int i = 0; i < byteSize; i++) {
            int dataOffset = start + (byteSize - i - 1);
            ret |= (data[dataOffset] & 0xFF) << (i << 3);
        }

        m_bitmaskPosition += size;
        return (ret >> offset) & bitmask;
    }

    public int tell() {
        return m_position;
    }

    public int tellBitmask() {
        return m_bitmaskPosition;
    }

    public void seek(int position) {
        m_position = position;
    }

    public void skip(int size) {
        m_position += size;
    }

    public void trim(int count) {
        int size = data.length - count;
        byte[] newData = new byte[size];
        System.arraycopy(data, 0, newData, 0, m_position);
        System.arraycopy(data, m_position + count, newData, m_position, data.length - m_position - count);
        data = newData;
    }

    public String readPaddedString() {
        if (readByte() == 0) {
            return readString();
        } else {
            throw new IllegalStateException("Padded String didn't begin with null byte!");
        }
    }

    public String readRSCString() {
        int length = readUnsignedByte();
        if (length >= 128) {
            m_position--;
            length = readUnsignedShort() - 32768;
        }
        byte[] byteData = new byte[length];
        int count = stringDecrypter.method240(data, 0, byteData, true, m_position, length);
        skip(count);
        return new String(byteData, 0, length);
    }

    public String readString() {
        int length = 0;
        if (data.length <= 1) {
            return "";
        }
        while (data[m_position + length] != '\0')
            length++;
        String ret;
        ret = new String(data, m_position, length);
        m_position += length + 1;
        return ret;
    }

    public BigInteger readUnsignedLong() {
        BigInteger bi = BigInteger.valueOf(readUnsignedInt()).shiftLeft(32);
        return bi.or(BigInteger.valueOf(readUnsignedInt()));
    }

    public long readUnsignedInt() {
        return (((long) readUnsignedByte()) << 24) | (readUnsignedByte() << 16) | (readUnsignedByte() << 8) | readUnsignedByte();
    }

    public int readUnsignedShort() {
        return (readUnsignedByte() << 8) | readUnsignedByte();
    }

    public int readUnsignedShortLE() {
        int a = readUnsignedByte();
        int b = readUnsignedByte() << 8;
        return b | a;
    }

    public byte readByte() {
        return data[m_position++];
    }

    public int readUnsignedByte() {
        return readByte() & 0xFF;
    }

    public void writeUnsignedByte(int value) {
        data[m_position++] = (byte) (value & 0xFF);
    }

    public void writeUnsignedShort(int value) {
        writeUnsignedByte(value >> 8);
        writeUnsignedByte(value);
    }

}

class ReplayPacketComparator implements Comparator<ReplayPacket> {

    @Override
    public int compare(ReplayPacket a, ReplayPacket b) {
        // this is reverse alphabetical order b/c we display them/in reverse order (y-=12 ea item)
        int offset = a.timestamp - b.timestamp;

        if (offset > 0) { // item a happened before item b
            offset = 10;
        } else if (offset < 0) { // item b happened before item a
            offset = -10;
            // items have the same name we would like to group items that are on the same tile as well,
            // not just having
            // the same name, so that we can use "last_item" in a useful way
        } else {
            int opcodeOffset = a.opcode - b.opcode;
            if (opcodeOffset > 0) {
                offset = -5;
            } else if (opcodeOffset < 0) {
                offset = 5;
            } else {
                offset = 0; // Could check data here to see if they are truly equal, but don't care right now
                Logger.Info("Packet had same opcode and timestamp as another packet!");
            }
        }
        return offset;
    }
}
