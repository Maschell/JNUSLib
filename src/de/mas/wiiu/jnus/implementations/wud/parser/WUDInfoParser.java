/****************************************************************************
 * Copyright (C) 2016-2018 Maschell
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ****************************************************************************/
package de.mas.wiiu.jnus.implementations.wud.parser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import de.mas.wiiu.jnus.Settings;
import de.mas.wiiu.jnus.entities.content.ContentFSTInfo;
import de.mas.wiiu.jnus.entities.fst.FST;
import de.mas.wiiu.jnus.entities.fst.FSTEntry;
import de.mas.wiiu.jnus.implementations.wud.reader.WUDDiscReader;
import de.mas.wiiu.jnus.utils.ByteUtils;
import de.mas.wiiu.jnus.utils.Utils;
import lombok.extern.java.Log;

@Log
// TODO: reduce magic numbers
public final class WUDInfoParser {
    public static byte[] DECRYPTED_AREA_SIGNATURE = new byte[] { (byte) 0xCC, (byte) 0xA6, (byte) 0xE6, 0x7B };
    public static byte[] PARTITION_FILE_TABLE_SIGNATURE = new byte[] { 0x46, 0x53, 0x54, 0x00 }; // "FST"
    public final static int PARTITION_TOC_OFFSET = 0x800;
    public final static int PARTITION_TOC_ENTRY_SIZE = 0x80;

    public static final String WUD_TMD_FILENAME = "title.tmd";
    public static final String WUD_TICKET_FILENAME = "title.tik";
    public static final String WUD_CERT_FILENAME = "title.cert";

    private WUDInfoParser() {
        //
    }

    public static WUDInfo createAndLoad(WUDDiscReader discReader, byte[] titleKey) throws IOException {
        WUDInfo result = new WUDInfo(titleKey, discReader);

        byte[] PartitionTocBlock = discReader.readDecryptedToByteArray(Settings.WIIU_DECRYPTED_AREA_OFFSET, 0, 0x8000, titleKey, null);

        // verify DiscKey before proceeding
        if (!Arrays.equals(Arrays.copyOfRange(PartitionTocBlock, 0, 4), DECRYPTED_AREA_SIGNATURE)) {
            log.info("Decryption of PartitionTocBlock failed");
            return null;
        }

        Map<String, WUDPartition> partitions = readPartitions(result, PartitionTocBlock);
        result.getPartitions().clear();
        result.getPartitions().putAll(partitions);

        return result;
    }

    private static Map<String, WUDPartition> readPartitions(WUDInfo wudInfo, byte[] partitionTocBlock) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(partitionTocBlock.length);

        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.put(partitionTocBlock);
        buffer.position(0);

        int partitionCount = (int) ByteUtils.getUnsingedIntFromBytes(partitionTocBlock, 0x1C, ByteOrder.BIG_ENDIAN);

        Map<String, WUDPartition> partitions = new HashMap<>();

        byte[] gamePartitionTMD = new byte[0];
        byte[] gamePartitionTicket = new byte[0];
        byte[] gamePartitionCert = new byte[0];

        String realGamePartitionName = null;
        // populate partition information from decrypted TOC
        for (int i = 0; i < partitionCount; i++) {

            int offset = (PARTITION_TOC_OFFSET + (i * PARTITION_TOC_ENTRY_SIZE));
            byte[] partitionIdentifier = Arrays.copyOfRange(partitionTocBlock, offset, offset + 0x19);
            int j = 0;
            for (j = 0; j < partitionIdentifier.length; j++) {
                if (partitionIdentifier[j] == 0) {
                    break;
                }
            }
            String partitionName = new String(Arrays.copyOfRange(partitionIdentifier, 0, j));

            // calculate partition offset (relative from WIIU_DECRYPTED_AREA_OFFSET) from decrypted TOC
            long tmp = ByteUtils.getUnsingedIntFromBytes(partitionTocBlock, (PARTITION_TOC_OFFSET + (i * PARTITION_TOC_ENTRY_SIZE) + 0x20),
                    ByteOrder.BIG_ENDIAN);

            long partitionOffset = ((tmp * (long) 0x8000) - 0x10000);

            WUDPartition partition = new WUDPartition(partitionName, partitionOffset);

            if (partitionName.startsWith("SI")) {
                byte[] fileTableBlock = wudInfo.getWUDDiscReader().readDecryptedToByteArray(Settings.WIIU_DECRYPTED_AREA_OFFSET + partitionOffset, 0, 0x8000,
                        wudInfo.getTitleKey(), null);
                if (!Arrays.equals(Arrays.copyOfRange(fileTableBlock, 0, 4), PARTITION_FILE_TABLE_SIGNATURE)) {
                    log.info("FST Decrpytion failed");
                    continue;
                }

                FST fst = FST.parseFST(fileTableBlock, null);

                byte[] rawTIK = getFSTEntryAsByte(WUD_TICKET_FILENAME, partition, fst, wudInfo.getWUDDiscReader(), wudInfo.getTitleKey());
                byte[] rawTMD = getFSTEntryAsByte(WUD_TMD_FILENAME, partition, fst, wudInfo.getWUDDiscReader(), wudInfo.getTitleKey());
                byte[] rawCert = getFSTEntryAsByte(WUD_CERT_FILENAME, partition, fst, wudInfo.getWUDDiscReader(), wudInfo.getTitleKey());

                gamePartitionTMD = rawTMD;
                gamePartitionTicket = rawTIK;
                gamePartitionCert = rawCert;

                // We want to use the real game partition
                realGamePartitionName = partitionName = "GM" + Utils.ByteArrayToString(Arrays.copyOfRange(rawTIK, 0x1DC, 0x1DC + 0x08));
            } else if (partitionName.startsWith(realGamePartitionName)) {
                wudInfo.setGamePartitionName(partitionName);
                partition = new WUDGamePartition(partitionName, partitionOffset, gamePartitionTMD, gamePartitionCert, gamePartitionTicket);
            }
            byte[] header = wudInfo.getWUDDiscReader().readEncryptedToByteArray(partition.getPartitionOffset() + 0x10000, 0, 0x8000);
            WUDPartitionHeader partitionHeader = WUDPartitionHeader.parseHeader(header);
            partition.setPartitionHeader(partitionHeader);

            partitions.put(partitionName, partition);
        }

        return partitions;
    }

    private static byte[] getFSTEntryAsByte(String filename, WUDPartition partition, FST fst, WUDDiscReader discReader, byte[] key) throws IOException {
        FSTEntry entry = getEntryByName(fst.getRoot(), filename);
        ContentFSTInfo info = fst.getContentFSTInfos().get((int) entry.getContentFSTID());

        // Calculating the IV
        ByteBuffer byteBuffer = ByteBuffer.allocate(0x10);
        byteBuffer.position(0x08);
        byte[] IV = byteBuffer.putLong(entry.getFileOffset() >> 16).array();

        return discReader.readDecryptedToByteArray(Settings.WIIU_DECRYPTED_AREA_OFFSET + (long) partition.getPartitionOffset() + (long) info.getOffset(),
                entry.getFileOffset(), (int) entry.getFileSize(), key, IV);
    }

    private static FSTEntry getEntryByName(FSTEntry root, String name) {
        for (FSTEntry cur : root.getFileChildren()) {
            if (cur.getFilename().equals(name)) {
                return cur;
            }
        }
        for (FSTEntry cur : root.getDirChildren()) {
            FSTEntry dir_result = getEntryByName(cur, name);
            if (dir_result != null) {
                return dir_result;
            }
        }
        return null;
    }
}
