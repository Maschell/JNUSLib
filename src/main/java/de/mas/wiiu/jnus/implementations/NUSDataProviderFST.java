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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import de.mas.wiiu.jnus.Settings;
import de.mas.wiiu.jnus.entities.FST.nodeentry.DirectoryEntry;
import de.mas.wiiu.jnus.entities.FST.nodeentry.FileEntry;
import de.mas.wiiu.jnus.entities.FST.nodeentry.NodeEntry;
import de.mas.wiiu.jnus.entities.TMD.Content;
import de.mas.wiiu.jnus.interfaces.ContentDecryptor;
import de.mas.wiiu.jnus.interfaces.FSTDataProvider;
import de.mas.wiiu.jnus.interfaces.NUSDataProvider;
import de.mas.wiiu.jnus.utils.FSTUtils;

public class NUSDataProviderFST implements NUSDataProvider {
    private final FSTDataProvider fstDataProvider;
    private final DirectoryEntry base;

    public NUSDataProviderFST(FSTDataProvider fstDataProvider, DirectoryEntry base) {
        this.base = base;
        this.fstDataProvider = fstDataProvider;
    }

    public NUSDataProviderFST(FSTDataProvider fstDataProvider, ContentDecryptor decryptor) {
        this(fstDataProvider, fstDataProvider.getRoot());
    }

    @Override
    public InputStream readRawContentAsStream(Content content, long offset, long size) throws IOException {
        String filename = content.getFilename();
        Optional<NodeEntry> contentFileOpt = FSTUtils.getChildOfDirectory(base, filename);
        NodeEntry contentFile = contentFileOpt.orElseThrow(() -> new FileNotFoundException(filename + " was not found."));
        if (!contentFile.isFile() || contentFile.isLink()) {
            throw new IOException("Requested file is not a file or is a link");
        }
        return fstDataProvider.readFileAsStream((FileEntry) contentFile, offset, size);
    }

    Map<Integer, Optional<byte[]>> h3Hashes = new HashMap<>();

    @Override
    public Optional<byte[]> getContentH3Hash(Content content) throws IOException {
        Optional<byte[]> res = h3Hashes.get(content.getID());
        if (res == null) {
            res = readFileByFilename(base, String.format("%08X%s", content.getID(), Settings.H3_EXTENTION));
            h3Hashes.put(content.getID(), res);
        }
        return res;
    }

    private Optional<byte[]> readFileByFilename(DirectoryEntry base, String filename) throws IOException {
        Optional<NodeEntry> entryOpt = FSTUtils.getChildOfDirectory(base, filename);
        if (entryOpt.isPresent()) {
            NodeEntry entry = entryOpt.get();
            if (!entry.isFile() || entry.isLink()) {
                return Optional.empty();
            }
            return Optional.of(fstDataProvider.readFile((FileEntry) entry));
        }
        return Optional.empty();
    }

    @Override
    public Optional<byte[]> getRawTMD() throws IOException {
        return readFileByFilename(base, Settings.TMD_FILENAME);
    }

    @Override
    public Optional<byte[]> getRawTicket() throws IOException {
        return readFileByFilename(base, Settings.TICKET_FILENAME);
    }

    @Override
    public Optional<byte[]> getRawCert() throws IOException {
        return readFileByFilename(base, Settings.CERT_FILENAME);
    }

    @Override
    public void cleanup() throws IOException {

    }

}
