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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Optional;

import de.mas.wiiu.jnus.entities.Ticket;
import de.mas.wiiu.jnus.entities.content.Content;
import de.mas.wiiu.jnus.utils.ByteArrayBuffer;
import de.mas.wiiu.jnus.utils.CheckSumWrongException;
import de.mas.wiiu.jnus.utils.HashUtil;
import de.mas.wiiu.jnus.utils.PipedInputStreamWithException;
import de.mas.wiiu.jnus.utils.StreamUtils;
import de.mas.wiiu.jnus.utils.Utils;
import lombok.extern.java.Log;

@Log
public class NUSDecryption extends AESDecryption {
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

    public void decryptFileStream(InputStream inputStream, OutputStream outputStream, long filesize, long fileOffset, short contentIndex, byte[] h3hash,
            long expectedSizeForHash) throws IOException, CheckSumWrongException {
        MessageDigest sha1 = null;
        MessageDigest sha1fallback = null;

        if (h3hash != null) {
            try {
                sha1 = MessageDigest.getInstance("SHA1");
                sha1fallback = MessageDigest.getInstance("SHA1");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }

        int BLOCKSIZE = 0x8000;
        // long dlFileLength = filesize;
        // if(dlFileLength > (dlFileLength/BLOCKSIZE)*BLOCKSIZE){
        // dlFileLength = ((dlFileLength/BLOCKSIZE)*BLOCKSIZE) +BLOCKSIZE;
        // }

        byte[] IV = new byte[0x10];

        IV[0] = (byte) ((contentIndex >> 8) & 0xFF);
        IV[1] = (byte) (contentIndex);

        byte[] blockBuffer = new byte[BLOCKSIZE];

        int inBlockBuffer;
        long written = 0;
        long writtenFallback = 0;

        int skipoffset = (int) (fileOffset % 0x8000);

        // If we are at the beginning of a block, but it's not the first one,
        // we need to get the IV from the last 16 bytes of the previous block.
        // while beeing paranoid to exactly read 16 bytes but not more. Reading more
        // would destroy our input stream.
        // The input stream has been prepared to start 16 bytes earlier on this case.
        if (fileOffset >= 0x8000 && fileOffset % 0x8000 == 0) {
            int toRead = 16;
            byte[] data = new byte[toRead];
            int readTotal = 0;
            while (readTotal < toRead) {
                int res = inputStream.read(data, readTotal, toRead - readTotal);
                StreamUtils.checkForException(inputStream);
                if (res < 0) {
                    // This should NEVER happen.
                    throw new IOException();
                }
                readTotal += res;
            }
            IV = Arrays.copyOfRange(data, 0, toRead);
        }

        ByteArrayBuffer overflow = new ByteArrayBuffer(BLOCKSIZE);

        // We can only decrypt multiples of 16. So we need to align it.
        long toRead = Utils.align(filesize + 15, 16);

        do {
            // In case we start on the middle of a block we need to consume the "garbage" and save the
            // current IV.
            if (skipoffset > 0) {
                int skippedBytes = StreamUtils.getChunkFromStream(inputStream, blockBuffer, overflow, skipoffset);
                if (skippedBytes >= 16) {
                    IV = Arrays.copyOfRange(blockBuffer, skippedBytes - 16, skippedBytes);
                }
                skipoffset = 0;
            }

            int curReadSize = BLOCKSIZE;
            if (toRead < BLOCKSIZE) {
                curReadSize = (int) toRead;
            }

            inBlockBuffer = StreamUtils.getChunkFromStream(inputStream, blockBuffer, overflow, curReadSize);

            byte[] output = decryptFileChunk(blockBuffer, (int) Utils.align(inBlockBuffer, 16), IV);

            if (inBlockBuffer == BLOCKSIZE) {
                IV = Arrays.copyOfRange(blockBuffer, BLOCKSIZE - 16, BLOCKSIZE);
            }

            int toWrite = inBlockBuffer;

            if ((written + inBlockBuffer) > filesize) {
                toWrite = (int) (filesize - written);
            }

            written += toWrite;
            toRead -= toWrite;

            outputStream.write(output, 0, toWrite);

            if (sha1 != null && sha1fallback != null) {
                sha1.update(output, 0, toWrite);

                // In some cases it's using the hash of the whole .app file instead of the part
                // that's been actually used.
                long toFallback = inBlockBuffer;
                if (writtenFallback + toFallback > expectedSizeForHash) {
                    toFallback = expectedSizeForHash - writtenFallback;
                }
                sha1fallback.update(output, 0, (int) toFallback);
                writtenFallback += toFallback;
            }
            if (written >= filesize && h3hash == null) {
                break;
            }
        } while (inBlockBuffer == BLOCKSIZE);

        if (sha1 != null && sha1fallback != null) {

            long missingInHash = expectedSizeForHash - writtenFallback;
            if (missingInHash > 0) {
                sha1fallback.update(new byte[(int) missingInHash]);
            }

            byte[] calculated_hash1 = sha1.digest();
            byte[] calculated_hash2 = sha1fallback.digest();
            byte[] expected_hash = h3hash;
            if (!Arrays.equals(calculated_hash1, expected_hash) && !Arrays.equals(calculated_hash2, expected_hash)) {
                inputStream.close();
                outputStream.close();
                throw new CheckSumWrongException("hash checksum failed", calculated_hash1, expected_hash);

            } else {
                log.finest("Hash DOES match saves output stream.");
            }
        }

        inputStream.close();
        outputStream.close();
    }

    public void decryptFileStreamHashed(InputStream inputStream, OutputStream outputStream, long filesize, long fileoffset, short contentIndex, byte[] h3Hash)
            throws IOException, CheckSumWrongException {
        int BLOCKSIZE = 0x10000;
        int HASHBLOCKSIZE = 0xFC00;

        long writeSize = HASHBLOCKSIZE;

        long block = (fileoffset / HASHBLOCKSIZE);
        long soffset = fileoffset - (fileoffset / HASHBLOCKSIZE * HASHBLOCKSIZE);

        if (soffset + filesize > writeSize) writeSize = writeSize - soffset;

        byte[] encryptedBlockBuffer = new byte[BLOCKSIZE];
        ByteArrayBuffer overflow = new ByteArrayBuffer(BLOCKSIZE);
        long wrote = 0;
        int inBlockBuffer = 0;
        do {
            inBlockBuffer = StreamUtils.getChunkFromStream(inputStream, encryptedBlockBuffer, overflow, BLOCKSIZE);

            if (writeSize > filesize) writeSize = filesize;

            byte[] output;
            try {
                output = decryptFileChunkHash(encryptedBlockBuffer, (int) block, contentIndex, h3Hash);
            } catch (CheckSumWrongException e) {
                outputStream.close();
                inputStream.close();
                throw e;
            }

            if ((wrote + writeSize) > filesize) {
                writeSize = (int) (filesize - wrote);
            }

            try {
                outputStream.write(output, (int) (0 + soffset), (int) writeSize);
            } catch (IOException e) {
                if (e.getMessage().equals("Pipe closed")) {
                    break;
                }
                e.printStackTrace();
                throw e;
            }
            wrote += writeSize;

            block++;

            if (soffset > 0) {
                writeSize = HASHBLOCKSIZE;
                soffset = 0;
            }
        } while (wrote < filesize && (inBlockBuffer == BLOCKSIZE));
        log.finest("Decryption okay");
        outputStream.close();
        inputStream.close();
    }

    private byte[] decryptFileChunkHash(byte[] blockBuffer, int block, int contentIndex, byte[] h3_hashes) throws CheckSumWrongException {
        int hashSize = 0x400;
        int blocksize = 0xFC00;
        byte[] IV = ByteBuffer.allocate(16).putShort((short) contentIndex).array();

        byte[] hashes = decryptFileChunk(blockBuffer, hashSize, IV);

        hashes[0] ^= (byte) ((contentIndex >> 8) & 0xFF);
        hashes[1] ^= (byte) (contentIndex & 0xFF);

        int H0_start = (block % 16) * 20;

        IV = Arrays.copyOfRange(hashes, H0_start, H0_start + 16);
        byte[] output = decryptFileChunk(blockBuffer, hashSize, blocksize, IV);

        HashUtil.checkFileChunkHashes(hashes, h3_hashes, output, block);

        return output;
    }

    public boolean decryptStreams(InputStream inputStream, OutputStream outputStream, long size, long offset, Content content, Optional<byte[]> h3HashHashed)
            throws IOException, CheckSumWrongException {

        short contentIndex = (short) content.getIndex();

        long encryptedFileSize = content.getEncryptedFileSize();

        if (content.isEncrypted()) {
            if (content.isHashed()) {
                byte[] h3 = h3HashHashed.orElseThrow(() -> new FileNotFoundException("h3 hash not found."));

                decryptFileStreamHashed(inputStream, outputStream, size, offset, (short) contentIndex, h3);
            } else {
                byte[] h3Hash = content.getSHA2Hash();
                // We want to check if we read the whole file or just a part of it.
                // There should be only one actual file inside a non-hashed content.
                // But it could also contain a directory, so we need to filter.
                long fstFileSize = content.getEntries().stream().filter(f -> !f.isDir()).findFirst().map(f -> f.getFileSize()).orElse(0L);
                if (size > 0 && size < fstFileSize) {
                    h3Hash = null;
                }
                decryptFileStream(inputStream, outputStream, size, offset, (short) contentIndex, h3Hash, encryptedFileSize);
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
}
