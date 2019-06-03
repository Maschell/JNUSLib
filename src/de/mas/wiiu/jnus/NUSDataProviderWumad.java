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
package de.mas.wiiu.jnus;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import de.mas.wiiu.jnus.entities.TMD;
import de.mas.wiiu.jnus.entities.content.Content;
import de.mas.wiiu.jnus.implementations.wud.parser.WUDPartitionHeader;
import de.mas.wiiu.jnus.implementations.wumad.WumadInfo;
import de.mas.wiiu.jnus.interfaces.NUSDataProvider;
import de.mas.wiiu.jnus.utils.StreamUtils;

public class NUSDataProviderWumad implements NUSDataProvider {

    private final WumadInfo info;

    private final Map<String, ZipEntry> files = new HashMap<>();

    public NUSDataProviderWumad(WumadInfo info) {
        this.info = info;
    }

    private Map<String, ZipEntry> loadFileList(ZipFile zipFile) {
        Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
        Map<String, ZipEntry> result = new HashMap<>();
        while (zipEntries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) zipEntries.nextElement();
            if (!entry.isDirectory()) {
                String entryName = entry.getName();
                result.put(entryName.toLowerCase(Locale.ENGLISH), entry);
            }
        }
        return result;
    }

    @Override
    public InputStream readContentAsStream(Content content, long offset, long size) throws IOException {
        if (files.isEmpty()) {
            files.putAll(loadFileList(info.getZipFile()));
        }

        ZipEntry entry = files.values().stream().filter(e -> e.getName().startsWith("p" + info.getPartition() + "."))
                .filter(e -> e.getName().endsWith(content.getFilename().toLowerCase())).findFirst().orElseThrow(() -> new FileNotFoundException());
        InputStream in = info.getZipFile().getInputStream(entry);
        StreamUtils.skipExactly(in, offset);

        return in;
    }

    @Override
    public Optional<byte[]> getContentH3Hash(Content content) throws IOException {
        WUDPartitionHeader partitionHeader = info.getPartitionHeader();
        if (!partitionHeader.isCalculatedHashes()) {
            try {
                partitionHeader.calculateHashes(TMD.parseTMD(getRawTMD().get()).getAllContents());
            } catch (ParseException e) {
                throw new IOException(e);
            }
        }
        return partitionHeader.getH3Hash(content);
    }

    @Override
    public Optional<byte[]> getRawTMD() throws IOException {
        return info.getTmdData();
    }

    @Override
    public Optional<byte[]> getRawTicket() throws IOException {
        return info.getTicketData();
    }

    @Override
    public Optional<byte[]> getRawCert() throws IOException {
        return info.getCertData();
    }

    @Override
    public void cleanup() throws IOException {
        info.getZipFile().close();
    }
}
