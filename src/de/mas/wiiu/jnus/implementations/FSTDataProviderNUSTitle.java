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
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.stream.Collectors;

import de.mas.wiiu.jnus.NUSTitle;
import de.mas.wiiu.jnus.entities.content.Content;
import de.mas.wiiu.jnus.entities.fst.FSTEntry;
import de.mas.wiiu.jnus.interfaces.FSTDataProvider;
import de.mas.wiiu.jnus.interfaces.HasNUSTitle;
import de.mas.wiiu.jnus.interfaces.NUSDataProvider;
import de.mas.wiiu.jnus.utils.CheckSumWrongException;
import de.mas.wiiu.jnus.utils.Utils;
import de.mas.wiiu.jnus.utils.cryptography.NUSDecryption;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

@Log
public class FSTDataProviderNUSTitle implements FSTDataProvider, HasNUSTitle {
    private final NUSTitle title;
    private final FSTEntry rootEntry;
    @Getter @Setter private String name;

    public FSTDataProviderNUSTitle(NUSTitle title) throws IOException {
        this.title = title;
        this.name = String.format("%016X", title.getTMD().getTitleID());

        if (title.getFST().isPresent()) {
            rootEntry = title.getFST().get().getRoot();
        } else if (title.getTMD().getContentCount() == 1) {
            // If the tmd has only one content file, it has not FST. We have to create our own FST.
            Content c = title.getTMD().getAllContents().values().stream().collect(Collectors.toList()).get(0);
            FSTEntry root = FSTEntry.getRootFSTEntry();
            FSTEntry.createFSTEntry(root, "data.bin", c); // Will add this title root.
            rootEntry = root;
        } else {
            throw new IOException("No FST root entry was found");
        }
    }

    @Override
    public FSTEntry getRoot() {
        return rootEntry;
    }

    @Override
    public boolean readFileToStream(OutputStream out, FSTEntry entry, long offset, long size) throws IOException {
        try {
            return decryptFSTEntryToStream(entry, out, offset, size);
        } catch (CheckSumWrongException | NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    private boolean decryptFSTEntryToStream(FSTEntry entry, OutputStream outputStream, long offset, long size)
            throws IOException, CheckSumWrongException, NoSuchAlgorithmException {
        if (entry.isNotInPackage() || !title.getTicket().isPresent()) {
            if (!title.getTicket().isPresent()) {
                log.info("Decryption not possible because no ticket was set.");
            } else if (entry.isNotInPackage()) {
                log.info("Decryption not possible because the FSTEntry is not in this package");
            }
            outputStream.close();
            return false;
        }

        Content c = title.getTMD().getContentByIndex(entry.getContentIndex());

        long payloadOffset = entry.getFileOffset() + offset;

        long streamOffset = payloadOffset;

        long streamFilesize = size;
        if (c.isHashed()) {
            streamOffset = (payloadOffset / 0xFC00) * 0x10000;
            long offsetInBlock = payloadOffset - ((streamOffset / 0x10000) * 0xFC00);
            if (offsetInBlock + size < 0xFC00) {
                streamFilesize = 0x10000L;
            } else {
                long curVal = 0x10000;
                long missing = (size - (0xFC00 - offsetInBlock));

                curVal += (missing / 0xFC00) * 0x10000;

                if (missing % 0xFC00 > 0) {
                    curVal += 0x10000;
                }

                streamFilesize = curVal;
            }
        } else {
            streamOffset = (payloadOffset / 0x8000) * 0x8000;
            // We need the previous IV if we don't start at the first block.
            if (payloadOffset >= 0x8000 && payloadOffset % 0x8000 == 0) {
                streamOffset -= 16;
            }
            streamFilesize = c.getEncryptedFileSize();
        }

        NUSDataProvider dataProvider = title.getDataProvider();

        InputStream in = dataProvider.getInputStreamFromContent(c, streamOffset, streamFilesize);

        try {
            NUSDecryption nusdecryption = new NUSDecryption(title.getTicket().get());
            Optional<byte[]> h3HashedOpt = Optional.empty();
            if (c.isHashed()) {
                h3HashedOpt = dataProvider.getContentH3Hash(c);
            }
            return nusdecryption.decryptStreams(in, outputStream, payloadOffset, size, c, h3HashedOpt, size != entry.getFileSize());
        } catch (CheckSumWrongException e) {
            if (c.isUNKNWNFlag1Set()) {
                log.info("Hash doesn't match. But file is optional. Don't worry.");
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("Hash doesn't match").append(System.lineSeparator());
                sb.append("Detailed info:").append(System.lineSeparator());
                sb.append(entry).append(System.lineSeparator());
                sb.append(String.format("%016x", title.getTMD().getTitleID()));
                sb.append(e.getMessage() + " Calculated Hash: " + Utils.ByteArrayToString(e.getGivenHash()) + ", expected hash: "
                        + Utils.ByteArrayToString(e.getExpectedHash()));
                log.info(sb.toString());
                throw e;
            }
        }
        return false;
    }

    @Override
    public NUSTitle getNUSTitle() {
        return title;
    }
}
