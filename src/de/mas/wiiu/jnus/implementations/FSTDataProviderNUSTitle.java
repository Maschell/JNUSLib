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
import java.util.Arrays;
import java.util.stream.Collectors;

import de.mas.wiiu.jnus.NUSTitle;
import de.mas.wiiu.jnus.entities.content.Content;
import de.mas.wiiu.jnus.entities.fst.FSTEntry;
import de.mas.wiiu.jnus.interfaces.FSTDataProvider;
import de.mas.wiiu.jnus.interfaces.HasNUSTitle;
import de.mas.wiiu.jnus.interfaces.NUSDataProvider;
import de.mas.wiiu.jnus.utils.CheckSumWrongException;
import de.mas.wiiu.jnus.utils.StreamUtils;
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
            root.addChildren(FSTEntry.createFSTEntry(root, "data.bin", c));
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

    private boolean decryptFSTEntryToStreamHashed(FSTEntry entry, OutputStream outputStream, long offset, long size)
            throws IOException, CheckSumWrongException, NoSuchAlgorithmException {
        Content c = title.getTMD().getContentByIndex(entry.getContentIndex());

        long payloadOffset = entry.getFileOffset() + offset;
        long streamOffset = payloadOffset;
        long streamFilesize = 0;

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

        NUSDataProvider dataProvider = title.getDataProvider();
        InputStream in = dataProvider.readContentAsStream(c, streamOffset, streamFilesize);

        NUSDecryption nusdecryption = new NUSDecryption(title.getTicket().get());

        return nusdecryption.decryptStreamsHashed(in, outputStream, payloadOffset, size, dataProvider.getContentH3Hash(c));
    }

    private boolean decryptFSTEntryToStreamNonHashed(FSTEntry entry, OutputStream outputStream, long offset, long size)
            throws IOException, CheckSumWrongException, NoSuchAlgorithmException {

        Content c = title.getTMD().getContentByIndex(entry.getContentIndex());

        byte[] IV = new byte[0x10];
        IV[0] = (byte) ((c.getIndex() >> 8) & 0xFF);
        IV[1] = (byte) (c.getIndex() & 0xFF);

        long payloadOffset = entry.getFileOffset() + offset;
        long streamOffset = payloadOffset;
        long streamFilesize = c.getEncryptedFileSize();

        // if we have an offset we can't calculate the hash anymore
        // we need a new IV
        if (streamOffset > 0) {
            streamFilesize = size;

            streamOffset -= 16;
            streamFilesize += 16;

            // We need to get the current IV as soon as we get the InputStream.
            IV = null;
        }

        NUSDataProvider dataProvider = title.getDataProvider();
        InputStream in = dataProvider.readContentAsStream(c, streamOffset, streamFilesize);

        if (IV == null) {
            // If we read with an offset > 16 we need the previous 16 bytes because they are the IV.
            // The input stream has been prepared to start 16 bytes earlier on this case.
            int toRead = 16;
            byte[] data = new byte[toRead];
            int readTotal = 0;
            while (readTotal < toRead) {
                int res = in.read(data, readTotal, toRead - readTotal);
                if (res < 0) {
                    // This should NEVER happen.
                    throw new IOException();
                }
                readTotal += res;
            }
            IV = Arrays.copyOfRange(data, 0, toRead);
        }
        NUSDecryption nusdecryption = new NUSDecryption(title.getTicket().get());

        return nusdecryption.decryptStreamsNonHashed(in, outputStream, payloadOffset, size, c, IV, size != entry.getFileSize());
    }

    private boolean decryptFSTEntryToStream(FSTEntry entry, OutputStream outputStream, long offset, long size)
            throws IOException, CheckSumWrongException, NoSuchAlgorithmException {
        if (entry.isNotInPackage()) {
            if (entry.isNotInPackage()) {
                log.info("Decryption not possible because the FSTEntry is not in this package");
            }
            outputStream.close();
            return false;
        }
        if (offset % 16 != 0) {
            throw new IOException("The offset for decryption need to be aligned to 16");
        }

        Content c = title.getTMD().getContentByIndex(entry.getContentIndex());

        try {
            if (c.isEncrypted()) {
                if (!title.getTicket().isPresent()) {
                    log.info("Decryption not possible because no ticket was set.");
                    outputStream.close();
                    return false;
                }
                if (c.isHashed()) {
                    return decryptFSTEntryToStreamHashed(entry, outputStream, offset, size);
                } else {
                    return decryptFSTEntryToStreamNonHashed(entry, outputStream, offset, size);
                }
            } else {
                InputStream in = title.getDataProvider().readContentAsStream(c, offset, size);

                try {
                    StreamUtils.saveInputStreamToOutputStreamWithHash(in, outputStream, size, c.getSHA2Hash(), c.getEncryptedFileSize(),
                            size != entry.getFileSize());
                    return true;
                } finally {
                    StreamUtils.closeAll(in, outputStream);
                }
            }
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
