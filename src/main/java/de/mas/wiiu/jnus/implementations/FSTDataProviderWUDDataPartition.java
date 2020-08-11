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
import java.util.Optional;

import de.mas.wiiu.jnus.entities.FST.nodeentry.FileEntry;
import de.mas.wiiu.jnus.entities.FST.nodeentry.RootEntry;
import de.mas.wiiu.jnus.entities.FST.sectionentry.SectionEntry;
import de.mas.wiiu.jnus.implementations.wud.content.partitions.WiiUDataPartition;
import de.mas.wiiu.jnus.implementations.wud.reader.WUDDiscReader;
import de.mas.wiiu.jnus.interfaces.FSTDataProvider;

public class FSTDataProviderWUDDataPartition implements FSTDataProvider {
    private final WiiUDataPartition partition;
    private final WUDDiscReader discReader;
    private final Optional<byte[]> discKey;

    public FSTDataProviderWUDDataPartition(WiiUDataPartition partition, WUDDiscReader discReader, Optional<byte[]> discKey) {
        this.partition = partition;
        this.discReader = discReader;
        this.discKey = discKey;
    }

    @Override
    public String getName() {
        return partition.getVolumeID();
    }

    @Override
    public RootEntry getRoot() {
        return partition.getFst().getRootEntry();
    }

    @Override
    public long readFileToStream(OutputStream out, FileEntry entry, long offset, long size) throws IOException {
        SectionEntry info = entry.getSectionEntry();
        if (!discKey.isPresent()) {
            return discReader.readEncryptedToStream(out,
                    partition.getSectionOffsetOnDefaultPartition() + info.getAddress().getAddressInBytes() + entry.getOffset() + offset, size);
        }
        return discReader.readDecryptedToOutputStream(out, partition.getSectionOffsetOnDefaultPartition() + info.getAddress().getAddressInBytes(),
                entry.getOffset() + offset, size, discKey.get(), null, false);
    }

    @Override
    public InputStream readFileAsStream(FileEntry entry, long offset, long size) throws IOException {
        if (!discKey.isPresent()) {
            SectionEntry info = entry.getSectionEntry();
            return discReader.readEncryptedToStream(
                    partition.getSectionOffsetOnDefaultPartition() + info.getAddress().getAddressInBytes() + entry.getOffset() + offset, size);
        }

        return FSTDataProvider.super.readFileAsStream(entry, offset, size);

    }

}
