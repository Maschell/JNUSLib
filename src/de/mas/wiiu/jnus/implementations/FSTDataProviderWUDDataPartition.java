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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.mas.wiiu.jnus.entities.content.ContentFSTInfo;
import de.mas.wiiu.jnus.entities.fst.FSTEntry;
import de.mas.wiiu.jnus.implementations.wud.parser.WUDDataPartition;
import de.mas.wiiu.jnus.implementations.wud.reader.WUDDiscReader;
import de.mas.wiiu.jnus.interfaces.FSTDataProvider;
import de.mas.wiiu.jnus.utils.FSTUtils;

public class FSTDataProviderWUDDataPartition implements FSTDataProvider {
    private final WUDDataPartition partition;
    private final WUDDiscReader discReader;
    private final byte[] titleKey;

    public FSTDataProviderWUDDataPartition(WUDDataPartition partition, WUDDiscReader discReader, byte[] titleKey) {
        this.partition = partition;
        this.discReader = discReader;
        this.titleKey = titleKey;
    }

    @Override
    public String getName() {
        return partition.getPartitionName();
    }

    @Override
    public FSTEntry getRoot() {
        return partition.getFST().getRoot();
    }

    @Override
    public byte[] readFile(FSTEntry entry, long offset, long size) throws IOException {
        ContentFSTInfo info = FSTUtils.getFSTInfoForContent(partition.getFST(), entry.getContentIndex())
                .orElseThrow(() -> new IOException("Failed to find FSTInfo"));
        return getChunkOfData(info.getOffset(), entry.getFileOffset() + offset, size, discReader, titleKey);
    }

    public byte[] getChunkOfData(long contentOffset, long fileoffset, long size, WUDDiscReader discReader, byte[] titleKey) throws IOException {
        if (titleKey == null) {
            return discReader.readEncryptedToByteArray(partition.getPartitionOffset() + contentOffset, fileoffset, (int) size);
        }
        return discReader.readDecryptedToByteArray(partition.getPartitionOffset() + contentOffset, fileoffset, (int) size, titleKey, null, false);
    }

    @Override
    public boolean readFileToStream(OutputStream out, FSTEntry entry, long offset, long size) throws IOException {
        ContentFSTInfo info = FSTUtils.getFSTInfoForContent(partition.getFST(), entry.getContentIndex())
                .orElseThrow(() -> new IOException("Failed to find FSTInfo"));
        if (titleKey == null) {
            return discReader.readEncryptedToStream(out, partition.getPartitionOffset() + info.getOffset() + entry.getFileOffset() + offset, size);
        }
        return discReader.readDecryptedToOutputStream(out, partition.getPartitionOffset() + info.getOffset(), entry.getFileOffset() + offset, size, titleKey,
                null, false);
    }

    @Override
    public InputStream readFileAsStream(FSTEntry entry, long offset, long size) throws IOException {
        if (titleKey == null) {
            ContentFSTInfo info = FSTUtils.getFSTInfoForContent(partition.getFST(), entry.getContentIndex())
                    .orElseThrow(() -> new IOException("Failed to find FSTInfo"));
            return discReader.readEncryptedToStream(partition.getPartitionOffset() + info.getOffset() + entry.getFileOffset() + offset, size);
        }

        return FSTDataProvider.super.readFileAsStream(entry, offset, size);

    }

}
