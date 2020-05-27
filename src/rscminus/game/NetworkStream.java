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

import rscminus.common.Crypto;
import rscminus.common.ISAACCipher;
import rscminus.common.MathUtil;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class NetworkStream {
    private ByteBuffer m_byteBuffer;
    private byte[] m_buffer;
    private int m_position;
    private int m_bitPosition;
    private int m_packetStart;

    public NetworkStream() {
        m_byteBuffer = ByteBuffer.allocate(5000);
        m_buffer = m_byteBuffer.array();
        m_position = 0;
        m_packetStart = 0;
    }

    public void startBitmask() {
        m_bitPosition = m_position << 3;
    }

    public void endBitmask() {
        m_position = (m_bitPosition + 7) >> 3;
    }

    public void writeBitmask(int size, int value) {
        int start = m_bitPosition >> 3;
        int bitEnd = m_bitPosition + size;
        int byteSize = ((bitEnd + 7) >> 3) - start;
        int offset = m_bitPosition - (start << 3);
        int shiftedValue = value << (32 - size - offset);
        int bitmask = MathUtil.getBitmask(size) << (32 - size - offset);

        for (int i = 0; i < byteSize; i++) {
            int byteShift = (24 - (i << 3));
            int mask = (bitmask >> byteShift) & 0xFF;
            int byteValue = (shiftedValue >> byteShift) & 0xFF;
            m_buffer[start + i] &= ~mask;
            m_buffer[start + i] |= byteValue & mask;
        }

        m_bitPosition += size;
    }

    public byte[] getByteArray() {
        return m_buffer;
    }

    public void flip() {
        m_position = 0;
    }

    public int getBufferSize() { return m_buffer.length; }

    public int getPosition() {
        return m_position;
    }

    public void setPosition(int position) {
        m_position = position;
    }

    public int getBitPosition() {
        return m_bitPosition;
    }

    public void setBitPosition(int position) {
        m_bitPosition = position;
    }

    public void writeOpcode(int value, ISAACCipher isaacCipher) {
        writeUnsignedByte((value + isaacCipher.getNextValue()) & 0xFF);
    }

    public void startPacket() {
        m_packetStart = m_position;
        writeUnsignedShort(0);
    }

    public void endPacket() {
        int length = m_position - m_packetStart - 2;
        if (length >= 160) {
            m_buffer[m_packetStart] = (byte)(length / 256 + 160);
            m_buffer[m_packetStart + 1] = (byte)(length & 0xFF);
        } else {
            m_buffer[m_packetStart] = (byte)length;
            if (length == 1)
                m_buffer[m_packetStart + 1] = m_buffer[m_packetStart + 2];
            else
                m_buffer[m_packetStart + 1] = m_buffer[m_packetStart + length + 1];
            m_position--;
        }
    }

    public int readOpcode(ISAACCipher isaacCipher) {
        return (readUnsignedByte() - isaacCipher.getNextValue()) & 0xFF;
    }

    public void writeArray(byte array[], int offset, int length) {
        System.arraycopy(array, offset, m_buffer, m_position, length);
        m_position += length;
    }

    public void writeByte(byte value) {
        m_buffer[m_position++] = value;
    }

    public void writeShort(short value) {
        writeUnsignedByte((value >> 8) & 0xFF);
        writeUnsignedByte(value & 0xFF);
    }

    public void writeUnsignedByte(int value) {
        m_buffer[m_position++] = (byte)value;
    }

    public void writeUnsignedShort(int value) {
        writeUnsignedByte((value >> 8) & 0xFF);
        writeUnsignedByte(value & 0xFF);
    }

    public void writeUnsignedShortInt(int value) {
        value &= Integer.MAX_VALUE;
        if (value <= Short.MAX_VALUE)
            writeUnsignedShort(value);
        else
            writeUnsignedInt(Integer.MIN_VALUE + value);
    }

    public void writeUnsignedInt(int value) {
        writeUnsignedByte((value >> 24) & 0xFF);
        writeUnsignedByte((value >> 16) & 0xFF);
        writeUnsignedByte((value >> 8) & 0xFF);
        writeUnsignedByte(value & 0xFF);
    }

    public void writeString(CharSequence charSequence) {
        for (int i=0; i < charSequence.length(); ++i)
            writeByte((byte)charSequence.charAt(i));

        writeUnsignedByte(0x00);
    }

    public void writePaddedString(String value) {
        writeUnsignedByte(0x00);
        writeString(value);
    }

    public void readArray(byte array[], int offset, int length) {
        System.arraycopy(m_buffer, m_position, array, offset, length);
        m_position += length;
    }

    public byte readByte() {
        return m_buffer[m_position++];
    }

    public short readShort() {
        return (short)((readUnsignedByte() << 8) | readUnsignedByte());
    }

    public int readUnsignedByte() {
        return m_buffer[m_position++] & 0xFF;
    }

    public int peekUnsignedByte() { return m_buffer[m_position] & 0xFF; }

    public int readUnsignedShort() {
        return (readUnsignedByte() << 8) | readUnsignedByte();
    }

    public int readUnsignedInt() {
        return (readUnsignedByte() << 24) | (readUnsignedByte() << 16) | (readUnsignedByte() << 8) | readUnsignedByte();
    }

    public String readString() {
        int length = 0;
        while(m_buffer[m_position + length] != '\0')
            length++;
        String ret;
        ret = new String(m_buffer, m_position, length);
        m_position += length + 1;
        return ret;
    }

    public String readUnicodeString() {
        int length = 0;
        while(m_buffer[m_position + length] != '\0')
            length++;
        String ret = "";
        try {
            ret = new String(m_buffer, m_position, length, "UTF8");
        } catch (Exception e) {
        }
        m_position += length + 1;
        return ret;
    }

    public void seek(int pos) {
        m_position = pos;
    }

    public void skip(int amount) {
        m_position += amount;
    }

    public void decryptRSA(int length) {
        byte newData[] = Crypto.decryptRSA(m_buffer, 0, length);
        System.arraycopy(newData, 0, m_buffer, 0, newData.length);
    }

    public void decryptXTEA(int length, int keys[]) {
        byte newData[] = Crypto.decryptXTEA(m_buffer, m_position, length, keys);
        System.arraycopy(newData, 0, m_buffer, m_position, newData.length);
    }

    public int readData(NetworkStream stream) {
        int length = readUnsignedShort();
        stream.seek(0);
        stream.writeArray(m_buffer, m_position, length);
        stream.seek(0);
        m_position += length;
        return length;
    }

    public int readVariableSize() {
        int messageLength = peekUnsignedByte();

        //The message length can be one or two bytes
        return messageLength < 128 ? readUnsignedByte() : readUnsignedShort() - 32768;
    }

    public int readPacket(NetworkStream stream) {
        int available = m_position;
        seek(0);

        if (available < 1) {
            m_position = available;
            return 0;
        }

        int length = readUnsignedByte();

        if (length >= 160) {
            if (available < 2) {
                m_position = available;
                return 0;
            }
            length = 256 * length - (40960 - readUnsignedByte());
        }

        int lengthSize = m_position;

        if ((available - lengthSize) < length) {
            m_position = available;
            return 0;
        }

        byte data[] = stream.getByteArray();
        if (length < 160) {
            stream.seek(0);
            System.arraycopy(m_buffer, m_position + 1, data, 0, length - 1);
            data[length - 1] = m_buffer[m_position];
        } else {
            stream.seek(0);
            System.arraycopy(m_buffer, m_position, data, 0, length);
        }

        int packetSize = length + lengthSize;
        m_position = available - packetSize;
        System.arraycopy(m_buffer, packetSize, m_buffer, 0, m_position);

        return length;
    }

    public void fill(SocketChannel socket) {
        m_byteBuffer.limit(5000);
        m_byteBuffer.position(m_position);
        try {
            socket.read(m_byteBuffer);
        } catch (Exception e) {
        }
        m_position = m_byteBuffer.position();
    }

    public void flush(SocketChannel socket) {
        if (m_position == 0)
            return;

        try {
            m_byteBuffer.limit(5000);
            m_byteBuffer.position(m_position);
            m_byteBuffer.flip();
            int length = socket.write(m_byteBuffer);
            m_position = 0;
        } catch (Exception e) {
        }
    }

    public void dump(String fname) {
        File f = new File(fname);
        try {
            DataOutputStream out = new DataOutputStream(new FileOutputStream(f));
            out.write(m_buffer, 0, m_position);
            out.close();
        } catch (Exception e) {
        }
    }
}
