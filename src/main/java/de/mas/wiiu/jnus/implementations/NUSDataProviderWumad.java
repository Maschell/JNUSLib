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
package de.mas.wiiu.jnus.implementations;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import de.mas.wiiu.jnus.entities.TMD.Content;
import de.mas.wiiu.jnus.implementations.wud.wumad.WumadGamePartition;
import de.mas.wiiu.jnus.interfaces.NUSDataProvider;
import de.mas.wiiu.jnus.utils.HashUtil;
import de.mas.wiiu.jnus.utils.StreamUtils;
import lombok.extern.java.Log;

@Log
public class NUSDataProviderWumad implements NUSDataProvider {
    private final ZipFile wumad;
    private final WumadGamePartition partition;

    private final Map<String, ZipEntry> files = new HashMap<>();

    public NUSDataProviderWumad(WumadGamePartition gamePartition, ZipFile wudmadFile) {
        this.wumad = wudmadFile;
        this.partition = gamePartition;
        files.putAll(loadFileList(wudmadFile));
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
    public InputStream readRawContentAsStream(Content content, long offset, long size) throws IOException {
        ZipEntry entry = files.values().stream().filter(e -> e.getName().startsWith("p" + partition.getPartitionName() + "."))
                .filter(e -> e.getName().endsWith(content.getFilename().toLowerCase())).findFirst().orElseThrow(() -> new FileNotFoundException());
        InputStream in = wumad.getInputStream(entry);
        StreamUtils.skipExactly(in, offset);

        return in;
    }

    @Override
    public Optional<byte[]> getContentH3Hash(Content content) throws IOException {
        byte[] hash = partition.getPartitionHeader().getH3HashArrayList().get(content.getIndex()).getH3HashArray();
        // Checking the hash of the h3 file.
        try {
            if (!Arrays.equals(HashUtil.hashSHA1(hash), content.getSHA2Hash())) {
                log.warning("h3 incorrect from WUD");
            }
        } catch (NoSuchAlgorithmException e) {
            log.warning(e.getMessage());
        }

        return Optional.of(hash);
    }

    @Override
    public Optional<byte[]> getRawTMD() throws IOException {
        return Optional.of(partition.getRawTMD());
    }

    @Override
    public Optional<byte[]> getRawTicket() throws IOException {
        return Optional.of(partition.getRawTicket());
    }

    @Override
    public Optional<byte[]> getRawCert() throws IOException {
        return Optional.of(partition.getRawCert());
    }

    @Override
    public void cleanup() throws IOException {
        wumad.close();
    }
}
