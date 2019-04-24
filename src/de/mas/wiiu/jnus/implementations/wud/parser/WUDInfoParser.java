/****************************************************************************
 * Copyright (C) 2016-2019 Maschell
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

import de.mas.wiiu.jnus.Settings;
import de.mas.wiiu.jnus.entities.content.ContentFSTInfo;
import de.mas.wiiu.jnus.entities.fst.FST;
import de.mas.wiiu.jnus.entities.fst.FSTEntry;
import de.mas.wiiu.jnus.implementations.wud.reader.WUDDiscReader;
import de.mas.wiiu.jnus.utils.ByteUtils;
import de.mas.wiiu.jnus.utils.FSTUtils;
import de.mas.wiiu.jnus.utils.Utils;
import lombok.val;
import lombok.extern.java.Log;

@Log
// TODO: reduce magic numbers
public final class WUDInfoParser {
    public static byte[] DECRYPTED_AREA_SIGNATURE = new byte[] { (byte) 0xCC, (byte) 0xA6, (byte) 0xE6, 0x7B };
    public static byte[] PARTITION_START_SIGNATURE = new byte[] { (byte) 0xCC, (byte) 0x93, (byte) 0xA4, (byte) 0xF5 };
    public static byte[] PARTITION_FILE_TABLE_SIGNATURE = new byte[] { 0x46, 0x53, 0x54, 0x00 }; // "FST"
    public final static int SECTOR_SIZE = 0x8000;
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

        byte[] PartitionTocBlock = readFromDisc(result, Settings.WIIU_DECRYPTED_AREA_OFFSET, SECTOR_SIZE);

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

            // calculate partition offset from decrypted TOC
            long offsetInSector = ByteUtils.getUnsingedIntFromBytes(partitionTocBlock, (PARTITION_TOC_OFFSET + (i * PARTITION_TOC_ENTRY_SIZE) + 0x20),
                    ByteOrder.BIG_ENDIAN);

            long partitionOffset = (offsetInSector * (long) SECTOR_SIZE);

            internalPartitions.put(partitionName, partitionOffset);
        }

        Map<String, WUDPartition> partitionsResult = new HashMap<>();

        val siPartitionOpt = internalPartitions.entrySet().stream().filter(e -> e.getKey().startsWith("SI")).findFirst();
        if (siPartitionOpt.isPresent()) {
            val siPartitionPair = siPartitionOpt.orElseThrow(() -> new ParseException("SI partition not found.", 0));

            // siPartition
            long siPartitionOffset = siPartitionPair.getValue();

            byte[] partitionHeaderData = readFromDisc(wudInfo, false, siPartitionOffset, 0x20);
            if (!Arrays.equals(Arrays.copyOf(partitionHeaderData, 0x4), PARTITION_START_SIGNATURE)) {
                throw new ParseException("Failed to get the partition data of the SI partition.", 0);
            }

            long headerSize = ByteUtils.getUnsingedIntFromBytes(partitionHeaderData, 0x04);

            long absoluteFSTOffset = siPartitionOffset + headerSize;
            long FSTSize = ByteUtils.getUnsingedIntFromBytes(partitionHeaderData, 0x14);

            byte[] fileTableBlock = readFromDisc(wudInfo, absoluteFSTOffset, FSTSize);

            if (!Arrays.equals(Arrays.copyOfRange(fileTableBlock, 0, 4), WUDInfoParser.PARTITION_FILE_TABLE_SIGNATURE)) {
                log.info("FST Decrpytion failed");
                throw new ParseException("Failed to decrypt the FST of the SI partition.", 0);
            }

            FST siFST = FST.parseFST(fileTableBlock, new HashMap<>());

            for (val dirChilden : siFST.getRoot().getDirChildren()) {
                // The SI partition contains the tmd, cert and tik for every GM partition.
                byte[] rawTIK = getFSTEntryAsByte(dirChilden.getFullPath() + '/' + WUD_TICKET_FILENAME, siPartitionOffset, headerSize, siFST,
                        wudInfo.getWUDDiscReader(), wudInfo.getTitleKey());
                byte[] rawTMD = getFSTEntryAsByte(dirChilden.getFullPath() + '/' + WUD_TMD_FILENAME, siPartitionOffset, headerSize, siFST,
                        wudInfo.getWUDDiscReader(), wudInfo.getTitleKey());
                byte[] rawCert = getFSTEntryAsByte(dirChilden.getFullPath() + '/' + WUD_CERT_FILENAME, siPartitionOffset, headerSize, siFST,
                        wudInfo.getWUDDiscReader(), wudInfo.getTitleKey());

                String partitionName = "GM" + Utils.ByteArrayToString(Arrays.copyOfRange(rawTIK, 0x1DC, 0x1DC + 0x08));

                val curPartitionOpt = internalPartitions.entrySet().stream().filter(e -> e.getKey().startsWith(partitionName)).findFirst();
                val curPartitionPair = curPartitionOpt.orElseThrow(() -> new ParseException("partition not found.", 0));
                long curPartitionOffset = curPartitionPair.getValue();

                byte[] curPartitionHeaderMeta = readFromDisc(wudInfo, false, curPartitionOffset, 0x20);

                if (!Arrays.equals(Arrays.copyOf(curPartitionHeaderMeta, 0x4), PARTITION_START_SIGNATURE)) {
                    throw new ParseException("Failed to decrypt the SI partition.", 0);
                }

                long curHeaderSize = ByteUtils.getUnsingedIntFromBytes(curPartitionHeaderMeta, 0x04);

                byte[] header = wudInfo.getWUDDiscReader().readEncryptedToByteArray(curPartitionOffset, 0, curHeaderSize);

                WUDPartitionHeader partitionHeader = WUDPartitionHeader.parseHeader(header);

                WUDGamePartition curPartition = new WUDGamePartition(curPartitionPair.getKey(), curPartitionOffset + curHeaderSize, partitionHeader, rawTMD,
                        rawCert, rawTIK);

                partitionsResult.put(curPartitionPair.getKey(), curPartition);
            }
        }

        val dataPartitions = internalPartitions.entrySet().stream().filter(e -> !e.getKey().startsWith("GM")).collect(Collectors.toList());
        for (val dataPartition : dataPartitions) {
            String curPartionName = dataPartition.getKey();
            long partitionOffset = dataPartition.getValue();

            byte[] partitionHeaderData = readFromDisc(wudInfo, false, partitionOffset, 0x20);

            if (!Arrays.equals(Arrays.copyOf(partitionHeaderData, 0x4), PARTITION_START_SIGNATURE)) {
                throw new ParseException("Failed to decrypt the " + curPartionName + " partition.", 0);
            }

            long headerSize = ByteUtils.getUnsingedIntFromBytes(partitionHeaderData, 0x04);

            long absoluteFSTOffset = partitionOffset + headerSize;
            long FSTSize = ByteUtils.getUnsingedIntFromBytes(partitionHeaderData, 0x14);

            byte[] curFileTableBlock = readFromDisc(wudInfo, absoluteFSTOffset, FSTSize);

            if (!Arrays.equals(Arrays.copyOfRange(curFileTableBlock, 0, 4), WUDInfoParser.PARTITION_FILE_TABLE_SIGNATURE)) {
                throw new IOException("FST Decrpytion failed");
            }

            FST curFST = FST.parseFST(curFileTableBlock, new HashMap<>());

            WUDDataPartition curDataPartition = new WUDDataPartition(curPartionName, partitionOffset + headerSize, curFST);

            partitionsResult.put(curPartionName, curDataPartition);
        }

        return partitionsResult.values();
    }

    private static byte[] readFromDisc(WUDInfo wudInfo, long offset, long size) throws IOException {
        return readFromDisc(wudInfo, wudInfo.getTitleKey() != null, offset, size);
    }

    private static byte[] readFromDisc(WUDInfo wudInfo, boolean decrypt, long offset, long size) throws IOException {
        if (!decrypt) {
            return wudInfo.getWUDDiscReader().readEncryptedToByteArray(offset, 0, size);
        } else {
            return wudInfo.getWUDDiscReader().readDecryptedToByteArray(offset, 0, size, wudInfo.getTitleKey(), null, true);
        }
    }

    private static byte[] getFSTEntryAsByte(String filePath, long partitionOffset, long headerSize, FST fst, WUDDiscReader discReader, byte[] key)
            throws IOException {
        FSTEntry entry = FSTUtils.getEntryByFullPath(fst.getRoot(), filePath).orElseThrow(() -> new FileNotFoundException(filePath + " was not found."));

        ContentFSTInfo info = fst.getContentFSTInfos().get((int) entry.getContentFSTID());

        if (key == null) {
            return discReader.readEncryptedToByteArray(headerSize + partitionOffset + (long) info.getOffset(), entry.getFileOffset(),
                    (int) entry.getFileSize());
        }

        // Calculating the IV
        ByteBuffer byteBuffer = ByteBuffer.allocate(0x10);
        byteBuffer.position(0x08);
        byte[] IV = byteBuffer.putLong(entry.getFileOffset() >> 16).array();

        return discReader.readDecryptedToByteArray(headerSize + partitionOffset + info.getOffset(), entry.getFileOffset(), (int) entry.getFileSize(), key, IV,
                false);
    }

}
