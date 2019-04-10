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
import java.util.Optional;

import de.mas.wiiu.jnus.entities.content.Content;
import de.mas.wiiu.jnus.implementations.wud.parser.WUDGamePartition;
import de.mas.wiiu.jnus.implementations.wud.parser.WUDPartitionHeader;
import de.mas.wiiu.jnus.implementations.wud.reader.WUDDiscReader;
import de.mas.wiiu.jnus.interfaces.NUSDataProvider;
import lombok.Getter;
import lombok.extern.java.Log;

@Log
public class NUSDataProviderWUD implements NUSDataProvider {
    @Getter private final WUDGamePartition gamePartition;
    @Getter private final WUDDiscReader discReader;

    public NUSDataProviderWUD(WUDGamePartition gamePartition, WUDDiscReader discReader) {
        this.gamePartition = gamePartition;
        this.discReader = discReader;
    }

    public long getOffsetInWUD(Content content) {
        if (content.getIndex() == 0) { // Index 0 is the FST which is at the beginning of the partion;
            return getGamePartition().getAbsolutePartitionOffset();
        }
        return getGamePartition().getAbsolutePartitionOffset() + content.getContentFSTInfo().getOffset();
    }

    @Override
    public byte[] getChunkFromContent(Content content, long offset, int size) throws IOException {
        return discReader.readEncryptedToByteArray(getOffsetInWUD(content), offset, size);
    }

    @Override
    public InputStream getInputStreamFromContent(Content content, long fileOffsetBlock, Optional<Long> size) throws IOException {
        WUDDiscReader discReader = getDiscReader();
        long offset = getOffsetInWUD(content) + fileOffsetBlock;
        long usedSize = content.getEncryptedFileSize() - fileOffsetBlock;
        if (size.isPresent()) {
            usedSize = size.get();
        }
        return discReader.readEncryptedToInputStream(offset, usedSize);
    }

    @Override
    public Optional<byte[]> getContentH3Hash(Content content) throws IOException {
        if (!getGamePartitionHeader().isCalculatedHashes()) {
            log.info("Calculating h3 hashes");
            getGamePartitionHeader().calculateHashes(getGamePartition().getTmd().getAllContents());
        }
        return getGamePartitionHeader().getH3Hash(content);
    }

    @Override
    public Optional<byte[]> getRawTMD() {
        return Optional.of(getGamePartition().getRawTMD());
    }

    @Override
    public Optional<byte[]> getRawTicket() {
        return Optional.of(getGamePartition().getRawTicket());
    }

    @Override
    public Optional<byte[]> getRawCert() throws IOException {
        return Optional.of(getGamePartition().getRawCert());
    }

    public WUDPartitionHeader getGamePartitionHeader() {
        return getGamePartition().getPartitionHeader();
    }

    @Override
    public void cleanup() {
        // We don't need it
    }

    @Override
    public String toString() {
        return "NUSDataProviderWUD [WUDGamePartition=" + gamePartition + "]";
    }
}
