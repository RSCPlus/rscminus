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
import rscminus.common.Logger;
import rscminus.common.Settings;
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
            Logger.Warn(e.getMessage() + " in ReplayEditor.importData. (This usually is because the replay is unplayable/broken)");
        }

        return true;
    }

    public void exportData(String fname, String originalDir) {
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

            //only copy keyboard.bin if no privacy things were checked
            //TODO: we could possibly search the sanitized text and remove it
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

            //copy mouse.bin without editing b/c nothing bad can be in it
            File mouseFile = new File(originalDir + "/mouse.bin.gz");
            if (mouseFile.exists()) {
                FileUtil.copyFile(mouseFile,new File(fname + "/mouse.bin.gz"));
            }

            //copy metadata.bin if it exists, otherwise we have to generate it based on the original replay, to maintain the time created, and to handle new metadata.bin format
            File metadataFile = new File(originalDir + "/metadata.bin");
            if (metadataFile.exists()) {
                FileUtil.copyFile(metadataFile,new File(fname + "/metadata.bin"));
                if (!addConverterSettingsToMetadata(fname)) {
                    FileUtil.copyFile(metadataFile,new File(String.format(fname + "/metadata.original.%d.bin",System.currentTimeMillis()/1000L)));
                    checkAndGenerateMetadata(originalDir, fname);
                }
            } else {
                //same function as RSC+ but we already checked metadata.exists()
                //and it saves metadata to different folder
                //and it writes converter settings
                checkAndGenerateMetadata(originalDir, fname);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void checkAndGenerateMetadata(String replayFolder, String outputFolder) {
        Logger.Info("Generating metadata for " + replayFolder);
        // generate new metadata
        int replayLength = Replay.getReplayEnding(new File(replayFolder + "/in.bin.gz"));
        long dateModified = new File(replayFolder + "/keys.bin").lastModified();

        try {
            DataOutputStream metadata =
                    new DataOutputStream(
                            new BufferedOutputStream(
                                    new FileOutputStream(new File(outputFolder + "/metadata.bin"))));
            metadata.writeInt(replayLength);
            metadata.writeLong(dateModified);

            //new bitmasked data in metadata.bin to denote what settings were used while converting
            byte converterSettings = 0;
            if (Settings.sanitizeFriendsIgnore)
                converterSettings += 4;
            if (Settings.sanitizePrivateChat)
                converterSettings += 2;
            if (Settings.sanitizePublicChat)
                converterSettings += 1;
            metadata.writeByte(converterSettings);

            metadata.flush();
            metadata.close();
        } catch (IOException e) {
            Logger.Error("Couldn't write metadata.bin!");
        }
    }

    public static boolean addConverterSettingsToMetadata(String outputFolder) {
        long metadataLength = new File(outputFolder + "/metadata.bin").length();
        int replayLength = -1;
        long dateModified = -1;
        byte previousConverterSettings = 0;

        // This is to prevent an old version of RSCMinus (this one) from destroying data in metadata.bin
        // We must also read the metadata.bin in case a replay is sent through the sanitizer more than once
        // 12 byte metadata.bin is original format, 13 byte is "converter settings format
        if (metadataLength == 12 || metadataLength == 13) {
            try {
                DataInputStream metadata =
                        new DataInputStream(
                                new BufferedInputStream(
                                        new FileInputStream(new File(outputFolder + "/metadata.bin"))));
                replayLength = metadata.readInt();
                dateModified = metadata.readLong();

                if (metadataLength == 13) {
                    // 13 byte metadata.bin is what this converter writes, so it must have been through the converter before
                    // That, or a new format metadata.bin exists with the convert settings padded
                    Logger.Debug("metadata.bin appears to already have converter settings written. Merging the settings.");
                    previousConverterSettings = metadata.readByte();
                }

                metadata.close();
            } catch (IOException e) {
                Logger.Error("Couldn't read metadata.bin!");
            }
        } else {
            //if metadata.bin exists, and it's not 12 or 13 bytes, then it's in a format that this version of RSCMinus can't handle.
            if (metadataLength != 0) {
                Logger.Error("@|red,intensity_bold Error, addConverterSettingsToMetadata cannot work on this file, generating a new one and saving the old one as metadata.original.<timestamp>.bin|@");
                Logger.Error("@|red,intensity_bold Please update RSCMinus|@");
            } else {
                Logger.Debug("metadata.bin existed but was 0 bytes...");
            }
            return false;
        }

        //add converter settings to end of metadata.bin
        try {
            DataOutputStream metadata =
                    new DataOutputStream(
                            new BufferedOutputStream(
                                    new FileOutputStream(new File(outputFolder + "/metadata.bin"))));
            metadata.writeInt(replayLength);
            metadata.writeLong(dateModified);

            //new bitmasked data in metadata.bin to denote what settings were used while converting
            byte converterSettings = 0;
            if (Settings.sanitizeFriendsIgnore)
                converterSettings += 4;
            if (Settings.sanitizePrivateChat)
                converterSettings += 2;
            if (Settings.sanitizePublicChat)
                converterSettings += 1;
            metadata.writeByte(converterSettings | previousConverterSettings);

            metadata.flush();
            metadata.close();
        } catch (IOException e) {
            Logger.Error("Couldn't write metadata.bin!");
        }

        return true;
    }

}
