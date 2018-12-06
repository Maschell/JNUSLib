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
import java.util.stream.Collectors;

import de.mas.wiiu.jnus.Settings;
import de.mas.wiiu.jnus.entities.content.ContentFSTInfo;
import de.mas.wiiu.jnus.entities.fst.FST;
import de.mas.wiiu.jnus.entities.fst.FSTEntry;
import de.mas.wiiu.jnus.implementations.wud.reader.WUDDiscReader;
import de.mas.wiiu.jnus.utils.ByteUtils;
import de.mas.wiiu.jnus.utils.Utils;
import lombok.val;
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

        byte[] PartitionTocBlock;
        if (titleKey == null) {
            PartitionTocBlock = discReader.readEncryptedToByteArray(Settings.WIIU_DECRYPTED_AREA_OFFSET, 0, 0x8000);
        } else {
            PartitionTocBlock = discReader.readDecryptedToByteArray(Settings.WIIU_DECRYPTED_AREA_OFFSET, 0, 0x8000, titleKey, null, true);
        }
        //

        // verify DiscKey before proceeding
        if (!Arrays.equals(Arrays.copyOfRange(PartitionTocBlock, 0, 4), DECRYPTED_AREA_SIGNATURE)) {
            // log.info("Decryption of PartitionTocBlock failed");
            throw new RuntimeException("Decryption of PartitionTocBlock failed");
        }

        result.getPartitions().clear();
        result.getPartitions().putAll(readGamePartitions(result, PartitionTocBlock));
        return result;
    }

    private static Map<String, WUDPartition> readGamePartitions(WUDInfo wudInfo, byte[] partitionTocBlock) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(partitionTocBlock.length);

        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.put(partitionTocBlock);
        buffer.position(0);

        int partitionCount = (int) ByteUtils.getUnsingedIntFromBytes(partitionTocBlock, 0x1C, ByteOrder.BIG_ENDIAN);

        Map<String, WUDPartition> internalPartitions = new HashMap<>();
        Map<String, WUDPartition> gamePartitions = new HashMap<>();

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

            byte[] header = wudInfo.getWUDDiscReader().readEncryptedToByteArray(partition.getPartitionOffset() + 0x10000, 0, 0x8000);
            WUDPartitionHeader partitionHeader = WUDPartitionHeader.parseHeader(header);
            partition.setPartitionHeader(partitionHeader);

            internalPartitions.put(partitionName, partition);
        }

        val siPartitionOpt = internalPartitions.entrySet().stream().filter(e -> e.getKey().startsWith("SI")).findFirst();
        val siPartitionPair = siPartitionOpt.orElseThrow(() -> new RuntimeException("SI partition not foud."));

        // siPartition
        long siPartitionOffset = siPartitionPair.getValue().getPartitionOffset();
        val siPartition = siPartitionPair.getValue();

        byte[] fileTableBlock;

        if (wudInfo.getTitleKey() == null) {
            fileTableBlock = wudInfo.getWUDDiscReader().readEncryptedToByteArray(Settings.WIIU_DECRYPTED_AREA_OFFSET + siPartitionOffset, 0, 0x8000);
        } else {
            fileTableBlock = wudInfo.getWUDDiscReader().readDecryptedToByteArray(Settings.WIIU_DECRYPTED_AREA_OFFSET + siPartitionOffset, 0, 0x8000,
                    wudInfo.getTitleKey(), null, true);
        }

        if (!Arrays.equals(Arrays.copyOfRange(fileTableBlock, 0, 4), PARTITION_FILE_TABLE_SIGNATURE)) {
            log.info("FST Decrpytion failed");
            throw new RuntimeException("Failed to decrypt the FST of the SI partition.");
        }

        FST siFST = FST.parseFST(fileTableBlock, null);

        for (val dirChilden : siFST.getRoot().getDirChildren()) {
            // The SI partition contains the tmd, cert and tik for every GM partition.
            byte[] rawTIK = getFSTEntryAsByte(dirChilden.getFullPath() + "\\" + WUD_TICKET_FILENAME, siPartition, siFST, wudInfo.getWUDDiscReader(),
                    wudInfo.getTitleKey());
            byte[] rawTMD = getFSTEntryAsByte(dirChilden.getFullPath() + "\\" + WUD_TMD_FILENAME, siPartition, siFST, wudInfo.getWUDDiscReader(),
                    wudInfo.getTitleKey());
            byte[] rawCert = getFSTEntryAsByte(dirChilden.getFullPath() + "\\" + WUD_CERT_FILENAME, siPartition, siFST, wudInfo.getWUDDiscReader(),
                    wudInfo.getTitleKey());

            String partitionName = "GM" + Utils.ByteArrayToString(Arrays.copyOfRange(rawTIK, 0x1DC, 0x1DC + 0x08));

            val curPartitionOpt = internalPartitions.entrySet().stream().filter(e -> e.getKey().startsWith(partitionName)).findFirst();
            val curPartitionPair = curPartitionOpt.orElseThrow(() -> new RuntimeException("partition not foud."));

            WUDGamePartition curPartition = new WUDGamePartition(curPartitionPair.getKey(), curPartitionPair.getValue().getPartitionOffset(), rawTMD, rawCert,
                    rawTIK);
            curPartition.setPartitionHeader(curPartitionPair.getValue().getPartitionHeader());
            gamePartitions.put(curPartitionPair.getKey(), curPartition);
        }

        val giPartitions = internalPartitions.entrySet().stream().filter(e -> e.getKey().startsWith("GI")).collect(Collectors.toList());
        for (val giPartition : giPartitions) {
            String curPartionName = giPartition.getKey();
            WUDPartition curPartition = giPartition.getValue();

            byte[] curFileTableBlock = wudInfo.getWUDDiscReader().readDecryptedToByteArray(
                    Settings.WIIU_DECRYPTED_AREA_OFFSET + curPartition.getPartitionOffset(), 0, 0x8000, wudInfo.getTitleKey(), null, true);
            if (!Arrays.equals(Arrays.copyOfRange(curFileTableBlock, 0, 4), WUDInfoParser.PARTITION_FILE_TABLE_SIGNATURE)) {
                log.info("FST Decrpytion failed");
                throw new RuntimeException("Failed to decrypt the FST of the SI partition.");
            }

            FST curFST = FST.parseFST(curFileTableBlock, null);

            WUDGIPartition curNewPartition = new WUDGIPartition(curPartionName, curPartition.getPartitionOffset(), curFST);
            curPartition.setPartitionHeader(curPartition.getPartitionHeader());

            gamePartitions.put(curPartionName, curNewPartition);
        }

        return gamePartitions;
    }

    private static byte[] getFSTEntryAsByte(String filePath, WUDPartition partition, FST fst, WUDDiscReader discReader, byte[] key) throws IOException {
        FSTEntry entry = getEntryByFullPath(fst.getRoot(), filePath);

        ContentFSTInfo info = fst.getContentFSTInfos().get((int) entry.getContentFSTID());

        if (key == null) {
            return discReader.readEncryptedToByteArray(Settings.WIIU_DECRYPTED_AREA_OFFSET + (long) partition.getPartitionOffset() + (long) info.getOffset(),
                    entry.getFileOffset(), (int) entry.getFileSize());
        }

        // Calculating the IV
        ByteBuffer byteBuffer = ByteBuffer.allocate(0x10);
        byteBuffer.position(0x08);
        byte[] IV = byteBuffer.putLong(entry.getFileOffset() >> 16).array();

        return discReader.readDecryptedToByteArray(Settings.WIIU_DECRYPTED_AREA_OFFSET + (long) partition.getPartitionOffset() + (long) info.getOffset(),
                entry.getFileOffset(), (int) entry.getFileSize(), key, IV, false);
    }

    private static FSTEntry getEntryByFullPath(FSTEntry root, String filePath) {
        for (FSTEntry cur : root.getFileChildren()) {
            if (cur.getFullPath().equals(filePath)) {
                return cur;
            }
        }
        for (FSTEntry cur : root.getDirChildren()) {
            FSTEntry dir_result = getEntryByFullPath(cur, filePath);
            if (dir_result != null) {
                return dir_result;
            }
        }
        return null;
    }
}
