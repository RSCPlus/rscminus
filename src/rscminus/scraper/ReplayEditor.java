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

import rscminus.common.FileUtil;
import rscminus.common.ISAACCipher;
import rscminus.common.Sleep;
import rscminus.common.Logger;
import rscminus.common.Settings;
import rscminus.scraper.client.Class11;
import rscminus.common.CRC16;

import java.io.*;
import java.util.Date;
import java.util.LinkedList;
import java.util.zip.GZIPOutputStream;

public class ReplayEditor {
    private LinkedList<ReplayKeyPair> m_keys = new LinkedList<ReplayKeyPair>();
    private LinkedList<ReplayPacket> m_incomingPackets = new LinkedList<ReplayPacket>();
    private LinkedList<ReplayPacket> m_outgoingPackets = new LinkedList<ReplayPacket>();
    private ReplayVersion m_replayVersion = new ReplayVersion();
    private ReplayMetadata m_replayMetadata = new ReplayMetadata();
    private boolean m_compressed = false;
    private byte[] m_inMetadata = new byte[32];
    private byte[] m_outMetadata = new byte[32];
    private byte[] m_inChecksum = new byte[32];
    private byte[] m_outChecksum = new byte[32];
    private byte[] m_metadata = new byte[1];
    public byte readConversionSettings = 0;

    private static final byte[] spoofedClientMAC = {(byte)0x00, (byte)0x00, (byte)0x00, (byte)0xCC, (byte)0xCC, (byte)0xCC};
    private static final byte[] spoofedServerMAC = {(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x55, (byte)0x55, (byte)0x55};

    public static final int VERSION = 5;
    public static boolean foundInauthentic = false;
    public static boolean appendingToReplay = false;

    public static final int METADATA_FLAGS_OFFSET = 0;
    public static final int FLAG_SANITIZE_PUBLIC = 0x01;
    public static final int FLAG_SANITIZE_PRIVATE = 0x02;
    public static final int FLAG_SANITIZE_FRIENDSIGNORES = 0x04;
    public static final int FLAG_SANITIZE_VERSION = 0x08;
    public static final int FLAG_SANITIZE_COPY_ONLY = 128;

    public static final int VIRTUAL_OPCODE_CONNECT = 10000;
    public static final int VIRTUAL_OPCODE_NOP = 10001;

    public byte[] getMetadata() {
        return m_metadata;
    }

    public LinkedList<ReplayPacket> getIncomingPackets() {
        return m_incomingPackets;
    }

    public LinkedList<ReplayPacket> getOutgoingPackets() {
        return m_outgoingPackets;
    }

    public ReplayVersion getReplayVersion() {
        return m_replayVersion;
    }

    public ReplayMetadata getReplayMetadata() {
        return m_replayMetadata;
    }

    public LinkedList<ReplayKeyPair> getKeyPairs() {
        return m_keys;
    }

    public int getKeyCRC() {
        CRC16 sum = new CRC16();
        byte[] keysbin = new byte[m_keys.size() * 16];
        int keysbinIndex = 0;
        for (int i = 0; i < m_keys.size(); i++) {
            for (int key : m_keys.get(i).keys) {
                keysbin[keysbinIndex++] = (byte)(key >>> 24 & 255);
                keysbin[keysbinIndex++] = (byte)(key >>> 16 & 255);
                keysbin[keysbinIndex++] = (byte)(key >>> 8 & 255);
                keysbin[keysbinIndex++] = (byte)(key & 255);
            }
        }
        sum.update(keysbin);
        return (int) sum.getValue();
    }

    public boolean authenticReplay() {
        if (foundInauthentic) {
            Logger.Info("foundInauthentic");
            foundInauthentic = false;
            return false;
        }

        if (m_replayVersion.clientVersion != 235) {
            Logger.Info("clientVersion != 235");
            return false;
        }

        if (m_replayVersion.version > 3) {
            Logger.Info("replayVersion > 3");
            return false;
        }

        if (readConversionSettings != -128) {
            Logger.Info(String.format("readConversionSettings != -128, instead == %d", readConversionSettings));
            return false;
        }

        if (m_replayVersion.version < 3) {
            return true;
        }

        for (int i = 0; i < m_inChecksum.length; i++) {
            if (m_inChecksum[i] != m_inMetadata[i]) {
                Logger.Info(String.format("bad in.bin checksum %d != %d, i = %d",m_inChecksum[i], m_inMetadata[i], i));
                return false;
            }
        }

        for (int i = 0; i < m_outChecksum.length; i++) {
            if (m_outChecksum[i] != m_outMetadata[i]) {
                Logger.Info("bad out.bin checksum");
                return false;
            }
        }

        return true;
    }

    public boolean importData(String fname) {
        // Required files
        File keysFile = new File(fname + "/keys.bin");
        File versionFile = new File(fname + "/version.bin");
        File inFile = new File(fname + "/in.bin.gz");
        File outFile = new File(fname + "/out.bin.gz");
        File metadataFile = new File(fname + "/metadata.bin");

        if (!inFile.exists())
            inFile = new File(fname + "/in.bin");
        if (!outFile.exists())
            outFile = new File(fname + "/out.bin");

        // If none of the required files exist, we can't continue
        if (!keysFile.exists())
            return false;

        if (versionFile.exists() && versionFile.length() < 8)
            return false;

        // Files can't be smaller than a certain size
        if (keysFile.length() < 16)
            return false;

        try {
            // Import metadata data
            if (metadataFile.exists() && metadataFile.length() >= 8) {
                DataInputStream metadata = new DataInputStream(new FileInputStream(metadataFile));
                m_replayMetadata.replayLength = metadata.readInt();
                m_replayMetadata.dateModified = metadata.readLong();

                long lastLegitDateModified = ((long)1533553582) * 1000; //2018 August 6th 11:06:22 UTC, a couple hours after close because of incorrect timezone on user computer
                if (m_replayMetadata.dateModified > lastLegitDateModified) {
                    foundInauthentic = true;
                    Logger.Warn(String.format("Inauthentic Date Modified %d > %d", m_replayMetadata.dateModified,lastLegitDateModified));
                }

                if (metadataFile.length() >= 13) {
                    m_replayMetadata.IPAddress1 = metadata.readInt();
                    m_replayMetadata.IPAddress2 = metadata.readInt();
                    m_replayMetadata.IPAddress3 = metadata.readInt();
                    m_replayMetadata.IPAddress4 = metadata.readInt();
                    m_replayMetadata.conversionSettings = metadata.readByte();
                    readConversionSettings = m_replayMetadata.conversionSettings;
                    m_replayMetadata.conversionSettings |= m_metadata[METADATA_FLAGS_OFFSET];
                    m_replayMetadata.userField = metadata.readInt();
                } else { //convert to metadata.bin v2
                    m_replayMetadata.conversionSettings = m_metadata[METADATA_FLAGS_OFFSET];
                    m_replayMetadata.userField = 0;
                }
                metadata.close();
            } else {
                m_replayMetadata.replayLength = 0; // This gets filled out properly in ReplayReader
                m_replayMetadata.dateModified = new Date().getTime(); // TODO: Replace this with correct dateModified
                m_replayMetadata.conversionSettings = m_metadata[METADATA_FLAGS_OFFSET];
                m_replayMetadata.userField = 0;
            }
        } catch (Exception e) {
        }

        try {
            // Import version data
            if (versionFile.exists()) {
                DataInputStream version = new DataInputStream(new FileInputStream(versionFile));
                m_replayVersion.version = version.readInt();
                m_replayVersion.clientVersion = version.readInt();
                version.close();
            } else {
                m_replayVersion.version = 0;
                m_replayVersion.clientVersion = 235;
            }

            // Import keys
            int keyCount = (int) keysFile.length() / 16;
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
        } catch (Exception e) {
            e.printStackTrace();
        }

        ReplayPacket replayPacket;
        try {
            // Import incoming packets
            if (inFile.exists()) {
                ReplayReader incomingReader = new ReplayReader();
                boolean success = incomingReader.open(inFile, m_replayVersion, m_replayMetadata, m_keys, m_inMetadata, m_metadata, m_inChecksum, false);
                if (!success)
                    return false;
                while ((replayPacket = incomingReader.readPacket(false)) != null) {
                    m_incomingPackets.add(replayPacket);
                }

                if (appendingToReplay) {
                    LinkedList<ReplayPacket> newPackets = ReplayClientExperiments.GenerateSpriteViewer(m_incomingPackets.getLast().timestamp);
                    for (int i = 0; i < newPackets.size(); i++) {
                        m_incomingPackets.add(newPackets.get(i));
                    }
                }
                //FileUtil.writeFull("output/in.raw", incomingReader.getData());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.Warn(e.getMessage() + " in ReplayEditor.importData. (This usually is because the replay is unplayable/broken)");
        }

        try {
            // Import outgoing packets
            if (outFile.exists()) {
                ReplayReader outgoingReader = new ReplayReader();
                boolean success = outgoingReader.open(outFile, m_replayVersion, m_replayMetadata, m_keys, m_outMetadata, m_metadata, m_outChecksum, true);
                if (!success)
                    return false;
                while ((replayPacket = outgoingReader.readPacket(false)) != null) {
                    m_outgoingPackets.add(replayPacket);
                }
                //FileUtil.writeFull("output/out.raw", outgoingReader.getData());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Skew disconnect timestamps
        boolean firstLogin = false;
        ReplayPacket previousPacket = null;
        int skew = 0;
        for (ReplayPacket packet : m_incomingPackets) {
            packet.timestamp += skew;
            if (packet.opcode == VIRTUAL_OPCODE_CONNECT) {
                if (firstLogin) {
                    int timestampDiff = packet.timestamp - previousPacket.timestamp;
                    if (timestampDiff <= 400) {
                        int offset = 401 - timestampDiff;
                        skew += offset;
                        packet.timestamp += offset;
                        System.out.println("WARNING: Skewing timestamps by +" + offset + " (" + (packet.timestamp - offset) + ":" + packet.timestamp + ")");
                    }
                }
                firstLogin = true;
            }
            previousPacket = packet;
        }

        return true;
    }

    public void justUpdateMetadata(String fname, String originalDir) {
            // Required files
            File keysFile = new File(fname + "/keys.bin");
            File versionFile = new File(fname + "/version.bin");
            File inFile = new File(fname + "/in.bin.gz");
            File outFile = new File(fname + "/out.bin.gz");
            File metadataFile = new File(fname + "/metadata.bin");

            try {

                if (keysFile.exists()) {
                    FileUtil.copyFile(keysFile, new File(fname + "/keys.bin"));
                }
                if (versionFile.exists()) {
                    FileUtil.copyFile(versionFile, new File(fname + "/version.bin"));
                }
                if (inFile.exists()) {
                    FileUtil.copyFile(inFile, new File(fname + "/in.bin.gz"));
                }
                if (outFile.exists()) {
                    FileUtil.copyFile(outFile, new File(fname + "/out.bin.gz"));
                }


                // Export metadata
                DataOutputStream metadata = new DataOutputStream(new FileOutputStream(metadataFile));
                metadata.writeInt(m_replayMetadata.replayLength);
                metadata.writeLong(m_replayMetadata.dateModified);
                setIPAddress();
                Logger.Info(String.format("ip: %d:%d:%d:%d",m_replayMetadata.IPAddress1,m_replayMetadata.IPAddress2,m_replayMetadata.IPAddress3,m_replayMetadata.IPAddress4));
                metadata.writeInt(m_replayMetadata.IPAddress1); // IPv6
                metadata.writeInt(m_replayMetadata.IPAddress2); // IPv6
                metadata.writeInt(m_replayMetadata.IPAddress3); // IPv6
                metadata.writeInt(m_replayMetadata.IPAddress4); // IPv4/IPv6
                metadata.writeByte(FLAG_SANITIZE_COPY_ONLY); //conversion settings; 128
                metadata.writeInt(m_replayMetadata.userField);
                metadata.close();

                File keyboardFile = new File(originalDir + "/keyboard.bin.gz");
                if (keyboardFile.exists()) {
                    FileUtil.copyFile(keyboardFile, new File(fname + "/keyboard.bin.gz"));
                }

                File mouseFile = new File(originalDir + "/mouse.bin.gz");
                if (mouseFile.exists()) {
                    FileUtil.copyFile(mouseFile,new File(fname + "/mouse.bin.gz"));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    public void exportData(String fname, String originalDir) {
        // Required files
        File keysFile = new File(fname + "/keys.bin");
        File versionFile = new File(fname + "/version.bin");
        File inFile = new File(fname + "/in.bin.gz");
        File outFile = new File(fname + "/out.bin.gz");
        File metadataFile = new File(fname + "/metadata.bin");

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

                    disconnectCount++;
                    in.writeInt(packet.timestamp);
                    in.writeInt(1);
                    in.writeByte(packet.data[0]);
                    if ((packet.data[0] & 64) != 0) {
                        isaac.reset();
                        isaac.setKeys(m_keys.get(++keyIndex).keys);
                    }
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

                if (packet.timestamp < lastTimestamp) {
                    System.out.println("Timestamp is in the past");
                }

                // Update metadata length
                m_replayMetadata.replayLength = packet.timestamp;

                lastTimestamp = packet.timestamp;
            }
            in.writeInt(ReplayReader.TIMESTAMP_EOF);
            if (m_replayVersion.version >= 3)
                in.write(m_inMetadata);
            in.write(m_metadata);
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
            if (m_replayVersion.version >= 3)
                out.write(m_outMetadata);
            out.write(m_metadata);
            out.close();

            // Export metadata
            DataOutputStream metadata = new DataOutputStream(new FileOutputStream(metadataFile));
            metadata.writeInt(m_replayMetadata.replayLength);
            metadata.writeLong(m_replayMetadata.dateModified);
            setIPAddress();
            Logger.Info(String.format("ip: %d:%d:%d:%d",m_replayMetadata.IPAddress1,m_replayMetadata.IPAddress2,m_replayMetadata.IPAddress3,m_replayMetadata.IPAddress4));
            metadata.writeInt(m_replayMetadata.IPAddress1); // IPv6
            metadata.writeInt(m_replayMetadata.IPAddress2); // IPv6
            metadata.writeInt(m_replayMetadata.IPAddress3); // IPv6
            metadata.writeInt(m_replayMetadata.IPAddress4); // IPv4/IPv6
            Logger.Info(String.format("conversionSettings: %d",m_replayMetadata.conversionSettings));
            metadata.writeByte(m_replayMetadata.conversionSettings);
            metadata.writeInt(m_replayMetadata.userField);
            metadata.close();
            
            // only copy keyboard.bin if no privacy settings are checked
            // TODO: we could possibly search the sanitized text and remove it
            //      from keyboard.bin.gz, if keyboard.bin.gz were more useful
            //      Would have to handle backspace though.

            if (!Settings.sanitizeFriendsIgnore &&
                !Settings.sanitizePublicChat &&
                !Settings.sanitizePrivateChat
            ) {
                File keyboardFile = new File(originalDir + "/keyboard.bin.gz");
                if (keyboardFile.exists()) {
                    FileUtil.copyFile(keyboardFile,new File(fname + "/keyboard.bin.gz"));
                }
            }

            // copy mouse.bin without editing b/c nothing privacy-sensitive can be in it
            File mouseFile = new File(originalDir + "/mouse.bin.gz");
            if (mouseFile.exists()) {
                FileUtil.copyFile(mouseFile,new File(fname + "/mouse.bin.gz"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setIPAddress() {
        if (m_replayMetadata.IPAddress4 == -1) {
            if (authenticReplay()) {
                // World 1: IP address 217.163.53.178
                // World 2: IP address 217.163.53.179
                // World 3: IP address 217.163.53.180
                // World 4: IP address 217.163.53.181
                // World 5: IP address 217.163.53.182
                m_replayMetadata.IPAddress4 = (217 << 24) + (163 << 16) + (53 << 8) + 177;

                int worldsExcluded = 0;
                for (int i=0; i < 5; i++) {
                    worldsExcluded += (m_replayMetadata.world_num_excluded >> i) & 0x01;
                }
                if (worldsExcluded == 4) { // Rare, but friends on every Classic world but your own, you can tell what world you're on.
                    Logger.Info(String.format("@|red Using the worldExcluded Method to determine IP Address! worldsExcluded: %d|@", m_replayMetadata.world_num_excluded));
                    int i;
                    for (i = 0; i <= 5; i++) {
                        if (((m_replayMetadata.world_num_excluded >> i) & 0x01) == 0) {
                            break;
                        }
                    }
                    m_replayMetadata.IPAddress4 += i + 1;
                    Scraper.ipFoundCount += 1;
                } else {
                    // Marks the IP as 217.163.53.0
                    // 217.163.53.0/24 is assigned to Jagex, so it's just a marker that the replay is believed to be on "some" Jagex server.
                    m_replayMetadata.IPAddress4 &= 0xFFFFFF00;
                }
            } else {
                // Non-authentic replay & no IP address imported
                m_replayMetadata.IPAddress4 = 0;
            }
        } else {
            if (authenticReplay() && m_replayMetadata.IPAddress4 > 0 && m_replayMetadata.IPAddress4 <= 5) {
                m_replayMetadata.IPAddress4 += (217 << 24) + (163 << 16) + (53 << 8) + 177;
                Scraper.ipFoundCount += 1;
            } else {
                Logger.Warn(String.format("authentic replay: %b, Scraper.ip_address: %d", authenticReplay(), m_replayMetadata.IPAddress4));
            }
        }
        Logger.Info(String.format("IP Address determined: %d.%d.%d.%d",
            (m_replayMetadata.IPAddress4 >> 24) & 0xFF,
                (m_replayMetadata.IPAddress4 >> 16) & 0xFF,
                (m_replayMetadata.IPAddress4 >> 8) & 0xFF,
                (m_replayMetadata.IPAddress4 & 0xFF)));

    }

    private int getLengthSize(int size) {
        if (size >= 160)
            return 2;
        return 1;
    }

    private void writeLength(DataOutputStream pcap, int size) throws IOException {
        if (size >= 160) {
            byte[] length = {(byte)(size / 256 + 160), (byte)(size & 0xFF)};
            pcap.write(length);
        } else {
            pcap.writeByte((byte)size);
        }
    }

    private void writePCAPPacket(DataOutputStream pcap, ReplayPacket packet, boolean outgoing) throws IOException {
        if (packet.opcode == VIRTUAL_OPCODE_NOP)
            return;

        int opcode = packet.opcode;
        int size = 1;
        int lengthSize = 0;
        if (opcode == VIRTUAL_OPCODE_CONNECT) {
            if (outgoing) {
                opcode = 0;
                if (packet.data != null)
                    size += packet.data.length;
                lengthSize = getLengthSize(size);
            } else {
                opcode = packet.data[0];
            }
        } else {
            if (packet.data != null)
                size += packet.data.length;
            lengthSize = getLengthSize(size);
        }

        int timestampMS = packet.timestamp * 20; // Convert timestamp
        int timestampSeconds = (int)((timestampMS + m_replayMetadata.dateModified) / 1000);
        long timestampMicro = ((long)timestampMS * 1000) % 1000000;

        pcap.writeInt(timestampSeconds); // Timestamp seconds
        pcap.writeInt((int)timestampMicro); // Timestamp microseconds
        pcap.writeInt(size + lengthSize + 19); // Saved length
        pcap.writeInt(size + lengthSize + 19); // Original length

        // Ethernet header
        if (outgoing) {
            pcap.write(spoofedServerMAC);
            pcap.write(spoofedClientMAC);
        } else {
            pcap.write(spoofedClientMAC);
            pcap.write(spoofedServerMAC);
        }
        pcap.writeShort(0x0);

        // rscminus Header
        pcap.writeByte(outgoing ? 1 : 0); // Client
        pcap.writeInt(packet.opcode);

        if (lengthSize > 0)
            writeLength(pcap, size);
        pcap.writeByte(opcode);
        if (size > 1)
            pcap.write(packet.data);
    }

    public void exportPCAP(String fname) {
        // Required files
        File pcapFile = new File(fname + ".pcap");
        try {
            DataOutputStream pcap = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(pcapFile)));

            // Write global header
            pcap.writeInt(0xa1b2c3d4); // Magic number
            pcap.writeShort(2); // Version major
            pcap.writeShort(4); // Version minor
            pcap.writeInt(0); // Timezone correction (UTC)
            pcap.writeInt(0); // Timestamp accuracy
            pcap.writeInt(65535); // Packet snapshot length
            pcap.writeInt(1); // Data link type (Ethernet)

            int outgoingIndex = 0;
            ReplayPacket outgoingPacket = null;
            for(ReplayPacket packet : m_incomingPackets) {
                if (outgoingIndex < m_outgoingPackets.size()) {
                    if (outgoingPacket == null)
                        outgoingPacket = m_outgoingPackets.get(outgoingIndex);
                    while (outgoingPacket.timestamp <= packet.timestamp) {
                        writePCAPPacket(pcap, outgoingPacket, true);
                        outgoingIndex++;
                        if (outgoingIndex >= m_outgoingPackets.size())
                            break;
                        outgoingPacket = m_outgoingPackets.get(outgoingIndex);
                    }
                }
                writePCAPPacket(pcap, packet, false);
            }

            // Write remaining outgoing packets
            while (outgoingIndex < m_outgoingPackets.size()) {
                ReplayPacket packet = m_outgoingPackets.get(outgoingIndex++);
                writePCAPPacket(pcap, packet, true);
            }

            pcap.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
