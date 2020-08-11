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
package de.mas.wiiu.jnus.utils.cryptography;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import de.mas.wiiu.jnus.entities.Ticket;
import de.mas.wiiu.jnus.interfaces.ContentDecryptor;
import de.mas.wiiu.jnus.utils.ByteArrayBuffer;
import de.mas.wiiu.jnus.utils.StreamUtils;
import de.mas.wiiu.jnus.utils.Utils;

public class NUSDecryption extends AESDecryption implements ContentDecryptor {
    public NUSDecryption(byte[] AESKey, byte[] IV) {
        super(AESKey, IV);
    }

    public NUSDecryption(Ticket ticket) {
        this(ticket.getDecryptedKey(), ticket.getIV());
    }

    private byte[] decryptFileChunk(byte[] blockBuffer, int BLOCKSIZE, byte[] IV) {
        return decryptFileChunk(blockBuffer, 0, BLOCKSIZE, IV);
    }

    private byte[] decryptFileChunk(byte[] blockBuffer, int offset, int BLOCKSIZE, byte[] IV) {
        if (IV != null) {
            setIV(IV);
            init();
        }
        return decrypt(blockBuffer, offset, BLOCKSIZE);
    }

    @Override
    public long readDecryptedContentToStreamHashed(InputStream in, OutputStream out, long offset, long size, long payloadOffset, byte[] h3_hashes)
            throws IOException {
        int BLOCKSIZE = 0x10000;
        int HASHEDBLOCKSIZE = 0xFC00;
        int HASHSIZE = BLOCKSIZE - HASHEDBLOCKSIZE;

        long block = (offset / BLOCKSIZE);
        long writeSize = BLOCKSIZE;

        long soffset = payloadOffset;

        byte[] encryptedBlockBuffer = new byte[BLOCKSIZE];
        ByteArrayBuffer overflow = new ByteArrayBuffer(BLOCKSIZE);

        long wrote = 0;
        int inBlockBuffer = 0;

        try {
            do {
                inBlockBuffer = StreamUtils.getChunkFromStream(in, encryptedBlockBuffer, overflow, BLOCKSIZE);
                if (inBlockBuffer < 0) {
                    return wrote;
                }
                if (inBlockBuffer != BLOCKSIZE) {
                    throw new IOException("wasn't able to read  " + BLOCKSIZE);
                }

                byte[] hashes = decryptFileChunk(encryptedBlockBuffer, HASHSIZE, new byte[16]);

                int H0_start = (int) (((int) block % 16) * 20);

                byte[] IV = Arrays.copyOfRange(hashes, H0_start, H0_start + 16);
                byte[] output = decryptFileChunk(encryptedBlockBuffer, HASHSIZE, HASHEDBLOCKSIZE, IV);

                try {
                    if (writeSize > size) {
                        writeSize = size;
                    }
                    if (writeSize + wrote > size) {
                        writeSize = size - wrote;
                    }

                    long toBeWritten = writeSize;

                    if (soffset <= HASHSIZE) {
                        long writeHashSize = HASHSIZE;
                        if (writeSize < HASHSIZE) {
                            writeHashSize = writeSize;
                        }
                        if (writeHashSize + soffset > HASHSIZE) {
                            writeHashSize = HASHSIZE - soffset;
                        }
                        out.write(hashes, (int) (0 + soffset), (int) writeHashSize);
                        wrote += writeHashSize;
                        toBeWritten -= writeHashSize;

                        if (toBeWritten > 0) {
                            if (toBeWritten > HASHEDBLOCKSIZE) {
                                toBeWritten = HASHEDBLOCKSIZE;
                                writeSize = toBeWritten - HASHEDBLOCKSIZE;
                            }
                            out.write(output, 0, (int) toBeWritten);
                            wrote += toBeWritten;
                        }
                    } else {
                        soffset -= 0x400;
                        long writeThisTime = writeSize;
                        if (writeSize + soffset > HASHEDBLOCKSIZE) {
                            writeThisTime = HASHEDBLOCKSIZE - soffset;
                        }
                        out.write(output, (int) (0 + soffset), (int) writeThisTime);
                        wrote += writeThisTime;
                    }
                    writeSize = BLOCKSIZE;
                } catch (IOException e) {
                    if (e.getMessage().equals("Pipe closed")) {
                        break;
                    }
                    e.printStackTrace();
                    throw e;
                }

                block++;

                if (soffset > 0) {
                    soffset = 0;
                }
            } while (wrote < size && (inBlockBuffer == BLOCKSIZE));
        } finally {
            StreamUtils.closeAll(in, out);
        }
        return wrote > 0 ? wrote : -1;
    }

    @Override
    public long readDecryptedContentToStreamNonHashed(InputStream inputStream, OutputStream outputStream, long offset, long size, long payloadOffset, byte[] IV)
            throws IOException {
        int BLOCKSIZE = 0x80000;

        byte[] blockBuffer = new byte[BLOCKSIZE];

        int inBlockBuffer;
        long written = 0;
        long read = 0;

        try {
            ByteArrayBuffer overflow = new ByteArrayBuffer(BLOCKSIZE);

            // We can only decrypt multiples of 16. So we need to align it.
            long toRead = Utils.align(size, 16);

            do {
                long writeOffset = Math.max(0, payloadOffset - read);
                int curReadSize = BLOCKSIZE;
                if (toRead < BLOCKSIZE) {
                    curReadSize = (int) (toRead + writeOffset);
                }
                inBlockBuffer = StreamUtils.getChunkFromStream(inputStream, blockBuffer, overflow, (int) Utils.align(curReadSize, 16));
                if (inBlockBuffer <= 0) {
                    break;
                }

                byte[] output = decryptFileChunk(blockBuffer, (int) Utils.align(inBlockBuffer, 16), IV);

                if (inBlockBuffer > 16) {
                    IV = Arrays.copyOfRange(blockBuffer, BLOCKSIZE - 16, BLOCKSIZE);
                }

                long writeLength = Math.min((output.length - writeOffset), (size - written));

                try {
                    read += inBlockBuffer;
                    outputStream.write(output, (int) writeOffset, (int) writeLength);
                    written += writeLength;
                    toRead -= writeLength;
                } catch (IOException e) {
                    if (e.getMessage().equals("Pipe closed")) {
                        break;
                    } else {
                        throw e;
                    }
                }
                if (written >= size) {
                    break;
                }
            } while (true);

        } finally {
            StreamUtils.closeAll(inputStream, outputStream);
        }
        return written > 0 ? written : -1;
    }
}
