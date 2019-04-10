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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import de.mas.wiiu.jnus.FSTUtils;
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

    public static WUDInfo createAndLoad(WUDDiscReader discReader, byte[] titleKey) throws IOException, ParseException {
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
            throw new ParseException("Decryption of PartitionTocBlock failed", 0);
        }

        result.getPartitions().addAll(parsePartitions(result, PartitionTocBlock));
        return result;
    }

    private static Collection<WUDPartition> parsePartitions(WUDInfo wudInfo, byte[] partitionTocBlock) throws IOException, ParseException {
        ByteBuffer buffer = ByteBuffer.allocate(partitionTocBlock.length);

        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.put(partitionTocBlock);
        buffer.position(0);

        int partitionCount = (int) ByteUtils.getUnsingedIntFromBytes(partitionTocBlock, 0x1C, ByteOrder.BIG_ENDIAN);

        Map<String, Long> internalPartitions = new HashMap<>();

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

            internalPartitions.put(partitionName, partitionOffset);
        }

        val siPartitionOpt = internalPartitions.entrySet().stream().filter(e -> e.getKey().startsWith("SI")).findFirst();
        val siPartitionPair = siPartitionOpt.orElseThrow(() -> new ParseException("SI partition not found.", 0));

        // siPartition
        long siPartitionOffset = siPartitionPair.getValue();

        byte[] fileTableBlock;

        if (wudInfo.getTitleKey() == null) {
            fileTableBlock = wudInfo.getWUDDiscReader().readEncryptedToByteArray(Settings.WIIU_DECRYPTED_AREA_OFFSET + siPartitionOffset, 0, 0x8000);
        } else {
            fileTableBlock = wudInfo.getWUDDiscReader().readDecryptedToByteArray(Settings.WIIU_DECRYPTED_AREA_OFFSET + siPartitionOffset, 0, 0x8000,
                    wudInfo.getTitleKey(), null, true);
        }

        if (!Arrays.equals(Arrays.copyOfRange(fileTableBlock, 0, 4), PARTITION_FILE_TABLE_SIGNATURE)) {
            log.info("FST Decrpytion failed");
            throw new ParseException("Failed to decrypt the FST of the SI partition.", 0);
        }

        FST siFST = FST.parseFST(fileTableBlock, null);

        Map<String, WUDPartition> partitionsResult = new HashMap<>();

        for (val dirChilden : siFST.getRoot().getDirChildren()) {
            // The SI partition contains the tmd, cert and tik for every GM partition.
            byte[] rawTIK = getFSTEntryAsByte(dirChilden.getFullPath() + File.separator + WUD_TICKET_FILENAME, siPartitionOffset, siFST,
                    wudInfo.getWUDDiscReader(), wudInfo.getTitleKey());
            byte[] rawTMD = getFSTEntryAsByte(dirChilden.getFullPath() + File.separator + WUD_TMD_FILENAME, siPartitionOffset, siFST,
                    wudInfo.getWUDDiscReader(), wudInfo.getTitleKey());
            byte[] rawCert = getFSTEntryAsByte(dirChilden.getFullPath() + File.separator + WUD_CERT_FILENAME, siPartitionOffset, siFST,
                    wudInfo.getWUDDiscReader(), wudInfo.getTitleKey());

            String partitionName = "GM" + Utils.ByteArrayToString(Arrays.copyOfRange(rawTIK, 0x1DC, 0x1DC + 0x08));

            val curPartitionOpt = internalPartitions.entrySet().stream().filter(e -> e.getKey().startsWith(partitionName)).findFirst();
            val curPartitionPair = curPartitionOpt.orElseThrow(() -> new ParseException("partition not foud.", 0));
            long curPartitionOffset = curPartitionPair.getValue();

            WUDGamePartition curPartition = new WUDGamePartition(curPartitionPair.getKey(), curPartitionOffset, rawTMD, rawCert, rawTIK);
            byte[] header = wudInfo.getWUDDiscReader().readEncryptedToByteArray(curPartition.getPartitionOffset() + 0x10000, 0, 0x8000);
            WUDPartitionHeader partitionHeader = WUDPartitionHeader.parseHeader(header);
            curPartition.setPartitionHeader(partitionHeader);
            partitionsResult.put(curPartitionPair.getKey(), curPartition);
        }

        val dataPartitions = internalPartitions.entrySet().stream().filter(e -> !e.getKey().startsWith("GM")).collect(Collectors.toList());
        for (val dataPartition : dataPartitions) {
            String curPartionName = dataPartition.getKey();
            long partitionOffset = dataPartition.getValue();

            byte[] curFileTableBlock;
            if (wudInfo.getTitleKey() == null) {
                curFileTableBlock = wudInfo.getWUDDiscReader().readEncryptedToByteArray(Settings.WIIU_DECRYPTED_AREA_OFFSET + partitionOffset, 0, 0x8000);
            } else {
                curFileTableBlock = wudInfo.getWUDDiscReader().readDecryptedToByteArray(Settings.WIIU_DECRYPTED_AREA_OFFSET + partitionOffset, 0, 0x8000,
                        wudInfo.getTitleKey(), null, true);
            }
            if (!Arrays.equals(Arrays.copyOfRange(curFileTableBlock, 0, 4), WUDInfoParser.PARTITION_FILE_TABLE_SIGNATURE)) {
                log.info("FST Decrpytion failed");
                throw new ParseException("Failed to decrypt the FST of the " + curPartionName + " partition.", 0);
            }

            FST curFST = FST.parseFST(curFileTableBlock, null);

            WUDDataPartition curDataPartition = new WUDDataPartition(curPartionName, partitionOffset, curFST);

            byte[] header = wudInfo.getWUDDiscReader().readEncryptedToByteArray(curDataPartition.getPartitionOffset() + 0x10000, 0, 0x8000);
            WUDPartitionHeader partitionHeader = WUDPartitionHeader.parseHeader(header);
            curDataPartition.setPartitionHeader(partitionHeader);

            partitionsResult.put(curPartionName, curDataPartition);
        }

        return partitionsResult.values();
    }

    private static byte[] getFSTEntryAsByte(String filePath, long partitionOffset, FST fst, WUDDiscReader discReader, byte[] key) throws IOException {
        FSTEntry entry = FSTUtils.getEntryByFullPath(fst.getRoot(), filePath).orElseThrow(() -> new FileNotFoundException(filePath + " was not found."));

        ContentFSTInfo info = fst.getContentFSTInfos().get((int) entry.getContentFSTID());

        if (key == null) {
            return discReader.readEncryptedToByteArray(((long) Settings.WIIU_DECRYPTED_AREA_OFFSET) + partitionOffset + (long) info.getOffset(),
                    entry.getFileOffset(), (int) entry.getFileSize());
        }

        // Calculating the IV
        ByteBuffer byteBuffer = ByteBuffer.allocate(0x10);
        byteBuffer.position(0x08);
        byte[] IV = byteBuffer.putLong(entry.getFileOffset() >> 16).array();

        return discReader.readDecryptedToByteArray(((long) Settings.WIIU_DECRYPTED_AREA_OFFSET) + partitionOffset + info.getOffset(), entry.getFileOffset(),
                (int) entry.getFileSize(), key, IV, false);
    }

}
