package rscminus.scraper;

import rscminus.common.ISAACCipher;
import rscminus.scraper.client.Class11;

import java.io.*;
import java.util.LinkedList;
import java.util.zip.GZIPOutputStream;

public class ReplayEditor {
    private LinkedList<ReplayKeyPair> m_keys = new LinkedList<ReplayKeyPair>();
    private LinkedList<ReplayPacket> m_incomingPackets = new LinkedList<ReplayPacket>();
    private LinkedList<ReplayPacket> m_outgoingPackets = new LinkedList<ReplayPacket>();
    private ReplayVersion m_replayVersion = new ReplayVersion();

    public static final int VIRTUAL_OPCODE_CONNECT = 10000;
    public static final int VIRTUAL_OPCODE_NOP = 10001;

    public LinkedList<ReplayPacket> getIncomingPackets() {
        return m_incomingPackets;
    }

    public LinkedList<ReplayPacket> getOutgoingPackets() {
        return m_outgoingPackets;
    }

    public ReplayVersion getReplayVersion() {
        return m_replayVersion;
    }

    public LinkedList<ReplayKeyPair> getKeyPairs() {
        return m_keys;
    }

    public boolean importData(String fname) {
        // Required files
        File keysFile = new File(fname + "/keys.bin");
        File versionFile = new File(fname + "/version.bin");
        File inFile = new File(fname + "/in.bin.gz");
        File outFile = new File(fname + "/out.bin.gz");

        // If none of the required files exist, we can't continue
        if (!keysFile.exists() || !versionFile.exists() || !inFile.exists() || !outFile.exists())
            return false;

        try {
            // Import version data
            DataInputStream version = new DataInputStream(new FileInputStream(versionFile));
            m_replayVersion.version = version.readInt();
            m_replayVersion.clientVersion = version.readInt();
            version.close();

            // Import keys
            int keyCount = (int)keysFile.length() / 16;
            DataInputStream keys = new DataInputStream(new FileInputStream(keysFile));
            for (int i = 0; i < keyCount; i++) {
                ReplayKeyPair keyPair = new ReplayKeyPair();
                keyPair.keys[0] = keys.readInt();
                keyPair.keys[1] = keys.readInt();
                keyPair.keys[2] = keys.readInt();
                keyPair.keys[3] = keys.readInt();
                m_keys.add(keyPair);
            }
            keys.close();

            // Import incoming packets
            ReplayReader incomingReader = new ReplayReader();
            incomingReader.open(inFile, m_replayVersion, m_keys);
            ReplayPacket replayPacket;
            while ((replayPacket = incomingReader.readPacket(false, null)) != null)
                m_incomingPackets.add(replayPacket);

            // Import outgoing packets
            LinkedList<Integer> disconnectTimestamps = incomingReader.getDisconnectTimestamps();
            ReplayReader outgoingReader = new ReplayReader();
            outgoingReader.open(outFile, m_replayVersion, m_keys);
            while ((replayPacket = outgoingReader.readPacket(true, disconnectTimestamps)) != null)
                m_outgoingPackets.add(replayPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    public void exportData(String fname) {
        // Required files
        File keysFile = new File(fname + "/keys.bin");
        File versionFile = new File(fname + "/version.bin");
        File inFile = new File(fname + "/in.bin.gz");
        File outFile = new File(fname + "/out.bin.gz");

        try {
            // Export version info
            DataOutputStream version = new DataOutputStream(new FileOutputStream(versionFile));
            version.writeInt(m_replayVersion.version);
            version.writeInt(m_replayVersion.clientVersion);
            version.close();

            // Export keys
            DataOutputStream keys = new DataOutputStream(new FileOutputStream(keysFile));
            for (int i = 0; i < m_keys.size(); i++) {
                ReplayKeyPair keyPair = m_keys.get(i);
                keys.writeInt(keyPair.keys[0]);
                keys.writeInt(keyPair.keys[1]);
                keys.writeInt(keyPair.keys[2]);
                keys.writeInt(keyPair.keys[3]);
            }
            keys.close();

            // Export incoming packets
            ISAACCipher isaac = new ISAACCipher();
            int keyIndex = -1;
            int disconnectCount = 0;
            int lastTimestamp = 0;
            DataOutputStream in = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(inFile))));
            for(ReplayPacket packet : m_incomingPackets) {
                if (packet.opcode == VIRTUAL_OPCODE_NOP) {
                    continue;
                }

                // Handle virtual packets
                if (packet.opcode == VIRTUAL_OPCODE_CONNECT) {
                    // Write disconnect
                    if (m_replayVersion.version > 0 && disconnectCount > 0) {
                        in.writeInt(lastTimestamp);
                        in.writeInt(-1);
                    }

                    System.out.println("timestamp: " + packet.timestamp + ", lastTimestamp: " + lastTimestamp);

                    disconnectCount++;
                    in.writeInt(packet.timestamp);
                    in.writeInt(1);
                    in.writeByte(packet.data[0]);
                    isaac.reset();
                    isaac.setKeys(m_keys.get(++keyIndex).keys);
                    continue;
                }

                // Write timestamp
                in.writeInt(packet.timestamp);

                // Handle normal packets
                int packetLength = 1;
                if (packet.data != null)
                    packetLength += packet.data.length;
                if (packetLength >= 160) {
                    in.writeInt(packetLength + 2);
                    in.writeByte(packetLength / 256 + 160);
                    in.writeByte(packetLength & 0xFF);
                } else {
                    in.writeInt(packetLength + 1);
                    in.writeByte(packetLength);
                }

                // Write data
                int encodedOpcode = (packet.opcode + isaac.getNextValue()) & 0xFF;
                if (packetLength == 1) {
                    in.writeByte(encodedOpcode);
                } else {
                    if (packetLength < 160) {
                        int dataSize = packetLength - 1;
                        in.writeByte(packet.data[dataSize - 1]);
                        in.writeByte(encodedOpcode);
                        if (dataSize > 1)
                            in.write(packet.data, 0, dataSize - 1);
                    } else {
                        in.writeByte(encodedOpcode);
                        in.write(packet.data, 0, packet.data.length);
                    }
                }
                lastTimestamp = packet.timestamp;
            }
            in.writeInt(ReplayReader.TIMESTAMP_EOF);
            in.close();

            // Export outgoing packets
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(outFile))));
            keyIndex = -1;
            for(ReplayPacket packet : m_outgoingPackets) {
                if (packet.opcode == VIRTUAL_OPCODE_NOP) {
                    continue;
                }

                // Write timestamp
                out.writeInt(packet.timestamp);

                // Handle normal packets
                int packetLength = 1;
                if (packet.data != null)
                    packetLength += packet.data.length;
                if (packetLength >= 160) {
                    out.writeInt(packetLength + 2);
                    out.writeByte(packetLength / 256 + 160);
                    out.writeByte(packetLength & 0xFF);
                } else {
                    out.writeInt(packetLength + 1);
                    out.writeByte(packetLength);
                }

                // Write data
                int encodedOpcode = packet.opcode;
                if (encodedOpcode == VIRTUAL_OPCODE_CONNECT) {
                    encodedOpcode = 0;
                    isaac.reset();
                    isaac.setKeys(m_keys.get(++keyIndex).keys);
                } else {
                    encodedOpcode = (encodedOpcode + isaac.getNextValue()) & 0xFF;
                }
                if (packetLength == 1) {
                    out.writeByte(encodedOpcode);
                } else {
                    if (packetLength < 160) {
                        int dataSize = packetLength - 1;
                        out.writeByte(packet.data[dataSize - 1]);
                        out.writeByte(encodedOpcode);
                        if (dataSize > 1)
                            out.write(packet.data, 0, dataSize - 1);
                    } else {
                        out.writeByte(encodedOpcode);
                        out.write(packet.data, 0, packet.data.length);
                    }
                }
                lastTimestamp = packet.timestamp;
            }
            out.writeInt(ReplayReader.TIMESTAMP_EOF);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
