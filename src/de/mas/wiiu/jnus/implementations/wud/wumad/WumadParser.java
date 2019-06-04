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
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import de.mas.wiiu.jnus.entities.fst.FST;
import de.mas.wiiu.jnus.entities.fst.FSTEntry;
import de.mas.wiiu.jnus.implementations.wud.GamePartitionHeader;
import de.mas.wiiu.jnus.implementations.wud.parser.WUDGamePartition;
import de.mas.wiiu.jnus.utils.FSTUtils;
import de.mas.wiiu.jnus.utils.StreamUtils;

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

            // TODO: some .wumad doesn't have SI files.
            ZipEntry fst = zipFile.getEntry(SI_FST_FILENAME);
            
            byte[] fstBytes = StreamUtils.getBytesFromStream(zipFile.getInputStream(fst), (int) fst.getSize());

            FST fstdd = FST.parseFST(fstBytes);

            for (FSTEntry dirRoot : fstdd.getRoot().getDirChildren()) {
                ZipEntry data = zipFile.getEntry(String.format("sip.s00%s.00000000.app", dirRoot.getFilename()));

                byte[] rawTMD = getFSTEntryAsByte(dirRoot.getFullPath() + "/" + WUD_TMD_FILENAME, dirRoot, zipFile, data)
                        .orElseThrow(() -> new FileNotFoundException());
                byte[] rawCert = getFSTEntryAsByte(dirRoot.getFullPath() + "/" + WUD_CERT_FILENAME, dirRoot, zipFile, data)
                        .orElseThrow(() -> new FileNotFoundException());
                byte[] rawTIK = getFSTEntryAsByte(dirRoot.getFullPath() + "/" + WUD_TICKET_FILENAME, dirRoot, zipFile, data)
                        .orElseThrow(() -> new FileNotFoundException());

                ZipEntry headerEntry = zipFile.getEntry(String.format("p%s.header.bin", dirRoot.getFilename()));

                byte[] header = StreamUtils.getBytesFromStream(zipFile.getInputStream(headerEntry), (int) headerEntry.getSize());

                WumadGamePartition curPartition = new WumadGamePartition(dirRoot.getFilename(), GamePartitionHeader.parseHeader(header), rawTMD, rawCert,
                        rawTIK);

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
