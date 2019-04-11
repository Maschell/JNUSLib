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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

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
    @Getter @Setter private String name;

    public FSTDataProviderNUSTitle(NUSTitle title) {
        this.title = title;
        this.name = String.format("%016X", title.getTMD().getTitleID());
    }

    @Override
    public FSTEntry getRoot() {
        return title.getFST().getRoot();
    }

    @Override
    public byte[] readFile(FSTEntry entry, long offset, long size) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        readFileToStream(out, entry, offset, Optional.of(size));

        return out.toByteArray();
    }

    @Override
    public void readFileToStream(OutputStream out, FSTEntry entry, long offset, Optional<Long> size) throws IOException {
        long fileOffset = entry.getFileOffset() + offset;
        long fileOffsetBlock = fileOffset;
        long usedSize = size.orElse(entry.getFileSize());

        if (entry.getContent().isHashed()) {
            fileOffsetBlock = (fileOffset / 0xFC00) * 0x10000;
        } else {
            fileOffsetBlock = (fileOffset / 0x8000) * 0x8000;
            // We need the previous IV if we don't start at the first block.
            if (fileOffset >= 0x8000 && fileOffset % 0x8000 == 0) {
                fileOffsetBlock -= 16;
            }
        }
        try {
            decryptFSTEntryToStream(entry, out, fileOffsetBlock, fileOffset, usedSize);
        } catch (CheckSumWrongException e) {
            throw new IOException(e);
        }
    }

    private boolean decryptFSTEntryToStream(FSTEntry entry, OutputStream outputStream, long fileOffsetBlock, long fileOffset, long fileSize)
            throws IOException, CheckSumWrongException {
        if (entry.isNotInPackage() || entry.getContent() == null) {
            outputStream.close();
            return false;
        }

        Content c = entry.getContent();

        NUSDataProvider dataProvider = title.getDataProvider();

        InputStream in = dataProvider.getInputStreamFromContent(c, fileOffsetBlock);

        try {
            return decryptStreams(in, outputStream, fileSize, fileOffset, c);
        } catch (CheckSumWrongException e) {
            if (entry.getContent().isUNKNWNFlag1Set()) {
                log.info("Hash doesn't match. But file is optional. Don't worry.");
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("Hash doesn't match").append(System.lineSeparator());
                sb.append("Detailed info:").append(System.lineSeparator());
                sb.append(entry).append(System.lineSeparator());
                sb.append(entry.getContent()).append(System.lineSeparator());
                sb.append(String.format("%016x", title.getTMD().getTitleID()));
                sb.append(e.getMessage() + " Calculated Hash: " + Utils.ByteArrayToString(e.getGivenHash()) + ", expected hash: "
                        + Utils.ByteArrayToString(e.getExpectedHash()));
                log.info(sb.toString());
                throw e;
            }
        }
        return false;
    }

    private boolean decryptStreams(InputStream inputStream, OutputStream outputStream, long size, long offset, Content content)
            throws IOException, CheckSumWrongException {
        NUSDecryption nusdecryption = new NUSDecryption(title.getTicket());
        short contentIndex = (short) content.getIndex();

        long encryptedFileSize = content.getEncryptedFileSize();

        if (content.isEncrypted()) {
            if (content.isHashed()) {
                NUSDataProvider dataProvider = title.getDataProvider();
                byte[] h3 = dataProvider.getContentH3Hash(content).orElseThrow(() -> new FileNotFoundException("h3 hash not found."));

                nusdecryption.decryptFileStreamHashed(inputStream, outputStream, size, offset, (short) contentIndex, h3);
            } else {
                try {
                    byte[] h3Hash = content.getSHA2Hash();
                    // We want to check if we read the whole file or just a part of it.
                    // There should be only one actual file inside a non-hashed content.
                    // But it could also contain a directory, so we need to filter.
                    long fstFileSize = content.getEntries().stream().filter(f -> !f.isDir()).findFirst().map(f -> f.getFileSize()).orElse(0L);
                    if (size > 0 && size < fstFileSize) {
                        h3Hash = null;
                    }
                    nusdecryption.decryptFileStream(inputStream, outputStream, size, offset, (short) contentIndex, h3Hash, encryptedFileSize);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            StreamUtils.saveInputStreamToOutputStreamWithHash(inputStream, outputStream, size, content.getSHA2Hash(), encryptedFileSize);
        }

        synchronized (inputStream) {
            inputStream.close();
        }
        synchronized (outputStream) {
            outputStream.close();
        }
        return true;
    }

    @Override
    public NUSTitle getNUSTitle() {
        return title;
    }
}
