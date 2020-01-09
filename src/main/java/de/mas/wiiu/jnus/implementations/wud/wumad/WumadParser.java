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
package de.mas.wiiu.jnus.implementations.wud.wumad;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import de.mas.wiiu.jnus.entities.fst.FST;
import de.mas.wiiu.jnus.entities.fst.FSTEntry;
import de.mas.wiiu.jnus.implementations.wud.GamePartitionHeader;
import de.mas.wiiu.jnus.utils.FSTUtils;
import de.mas.wiiu.jnus.utils.StreamUtils;
import lombok.val;

public class WumadParser {

    public static final String WUD_TMD_FILENAME = "title.tmd";
    public static final String WUD_TICKET_FILENAME = "title.tik";
    public static final String WUD_CERT_FILENAME = "title.cert";

    public static final String SI_FST_FILENAME = "sip.fst.00000000.app";
    public static final String P01_HEADER = "";

    public static WumadInfo createWumadInfo(File wumadFile) throws IOException, ParserConfigurationException, SAXException, ParseException {
        WumadInfo result = new WumadInfo();

        try {
            ZipFile zipFile = new ZipFile(wumadFile);
            result.setZipFile(zipFile);

            // Let's get all possible partitions
            Map<String, List<ZipEntry>> allPartitions = zipFile.stream().filter(e -> e.getName().startsWith("p"))
                    .collect(Collectors.groupingBy(e -> e.getName().substring(1, 3)));

            Map<String, FSTEntry> gamepartitions = new HashMap<>();

            // If we have a SI partition, let parse the FST to get all game partitions.
            ZipEntry siFST = zipFile.getEntry(SI_FST_FILENAME);
            if (siFST != null) {
                byte[] fstBytes = StreamUtils.getBytesFromStream(zipFile.getInputStream(siFST), (int) siFST.getSize());

                FST parsedFST = FST.parseFST(fstBytes);
                gamepartitions.putAll(parsedFST.getRoot().getDirChildren().stream().collect(Collectors.toMap(e -> e.getFilename(), e -> e)));
            }

            // process all game partitions. Remove the partitions from the "all partitions" list on success.
            for (val e : gamepartitions.entrySet()) {
                ZipEntry data = zipFile.getEntry(String.format("sip.s00%s.00000000.app", e.getKey()));

                byte[] rawTMD = getFSTEntryAsByte("/" + e.getKey() + "/" + WUD_TMD_FILENAME, e.getValue(), zipFile, data)
                        .orElseThrow(() -> new FileNotFoundException());
                byte[] rawCert = getFSTEntryAsByte("/" + e.getKey() + "/" + WUD_CERT_FILENAME, e.getValue(), zipFile, data)
                        .orElseThrow(() -> new FileNotFoundException());
                byte[] rawTIK = getFSTEntryAsByte("/" + e.getKey() + "/" + WUD_TICKET_FILENAME, e.getValue(), zipFile, data)
                        .orElseThrow(() -> new FileNotFoundException());

                ZipEntry headerEntry = zipFile.getEntry(String.format("p%s.header.bin", e.getKey()));

                byte[] header = StreamUtils.getBytesFromStream(zipFile.getInputStream(headerEntry), (int) headerEntry.getSize());

                WumadGamePartition curPartition = new WumadGamePartition(e.getKey(), GamePartitionHeader.parseHeader(header), rawTMD, rawCert, rawTIK);

                result.getPartitions().add(curPartition);
                allPartitions.remove(e.getKey());
            }

            // The remaining partitions are data partitions.
            for (val e : allPartitions.entrySet()) {
                ZipEntry fstEntry = e.getValue().stream().filter(f -> f.getName().contains("fst")).findFirst().orElseThrow(() -> new FileNotFoundException());

                byte[] fstBytes = StreamUtils.getBytesFromStream(zipFile.getInputStream(fstEntry), (int) fstEntry.getSize());

                FST parsedFST = FST.parseFST(fstBytes);

                WumadDataPartition curPartition = new WumadDataPartition(e.getKey(), parsedFST);

                result.getPartitions().add(curPartition);

            }

        } catch (ZipException e) {
            e.printStackTrace();
            throw new ParseException("Failed to parse wumad:" + e.getMessage(), 0);
        }

        return result;
    }

    private static Optional<byte[]> getFSTEntryAsByte(String filePath, FSTEntry dirRoot, ZipFile zipFile, ZipEntry data) throws IOException {
        Optional<FSTEntry> entryOpt = FSTUtils.getEntryByFullPath(dirRoot, filePath);
        if (!entryOpt.isPresent()) {
            return Optional.empty();
        }
        FSTEntry entry = entryOpt.get();

        InputStream in = zipFile.getInputStream(data);

        StreamUtils.skipExactly(in, entry.getFileOffset());
        return Optional.of(StreamUtils.getBytesFromStream(in, (int) entry.getFileSize()));
    }
}
