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
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Optional;

import de.mas.wiiu.jnus.entities.FST.FST;
import de.mas.wiiu.jnus.entities.FST.sectionentry.SectionEntry;
import de.mas.wiiu.jnus.entities.TMD.Content;
import de.mas.wiiu.jnus.implementations.wud.content.partitions.WiiUGMPartition;
import de.mas.wiiu.jnus.implementations.wud.reader.WUDDiscReader;
import de.mas.wiiu.jnus.interfaces.NUSDataProvider;
import de.mas.wiiu.jnus.utils.FSTUtils;
import de.mas.wiiu.jnus.utils.HashUtil;
import de.mas.wiiu.jnus.utils.Utils;
import de.mas.wiiu.jnus.utils.blocksize.AddressInVolumeBlocks;
import de.mas.wiiu.jnus.utils.blocksize.SizeInVolumeBlocks;
import lombok.Getter;
import lombok.var;
import lombok.extern.java.Log;

@Log
public class NUSDataProviderWUD implements NUSDataProvider {
    @Getter private final WiiUGMPartition gamePartition;
    @Getter private final WUDDiscReader discReader;
    @Getter private FST fst;

    public NUSDataProviderWUD(WiiUGMPartition gamePartition, WUDDiscReader discReader) {
        this.gamePartition = gamePartition;
        this.discReader = discReader;
    }

    @Override
    public void setFST(FST fst) {
        // We need to set the correct blocksizes
        var blockSize = gamePartition.getVolumes().values().iterator().next().getBlockSize();
        for (SectionEntry e : fst.getSectionEntries()) {
            e.setAddress(new AddressInVolumeBlocks(blockSize, e.getAddress().getValue()));
            e.setSize(new SizeInVolumeBlocks(blockSize, e.getSize().getValue()));
        }
        this.fst = fst;
    }

    public long getOffsetInWUD(Content content) throws IOException {
        if (content.getIndex() == 0) { // Index 0 is the FST which is at the beginning of the partition;
            var vh = getGamePartition().getVolumes().values().iterator().next();
            return getGamePartition().getSectionOffsetOnDefaultPartition() + vh.getFSTAddress().getAddressInBytes();
        }
        SectionEntry info = FSTUtils.getSectionEntryForIndex(fst, content.getIndex()).orElseThrow(() -> new IOException("Failed to find FSTInfo"));
        return getGamePartition().getSectionOffsetOnDefaultPartition() + info.getAddress().getAddressInBytes();
    }

    @Override
    public InputStream readRawContentAsStream(Content content, long fileOffsetBlock, long size) throws IOException {
        WUDDiscReader discReader = getDiscReader();
        long offset = getOffsetInWUD(content) + fileOffsetBlock;

        return discReader.readEncryptedToStream(offset, Utils.align(size, 16));
    }

    @Override
    public Optional<byte[]> getContentH3Hash(Content content) throws IOException {
        byte[] hash = getGamePartition().getVolumes().values().iterator().next().getH3HashArrayList().get(content.getIndex()).getH3HashArray();
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

    @Override
    public void cleanup() {
        // We don't need it
    }

    @Override
    public String toString() {
        return "NUSDataProviderWUD [WUDGamePartition=" + gamePartition + "]";
    }
}
