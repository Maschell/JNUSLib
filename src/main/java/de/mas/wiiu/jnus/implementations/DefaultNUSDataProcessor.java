package de.mas.wiiu.jnus.implementations;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Optional;

import de.mas.wiiu.jnus.entities.TMD.Content;
import de.mas.wiiu.jnus.interfaces.ContentDecryptor;
import de.mas.wiiu.jnus.interfaces.NUSDataProcessor;
import de.mas.wiiu.jnus.interfaces.NUSDataProvider;
import de.mas.wiiu.jnus.utils.ByteArrayBuffer;
import de.mas.wiiu.jnus.utils.CheckSumWrongException;
import de.mas.wiiu.jnus.utils.HashUtil;
import de.mas.wiiu.jnus.utils.PipedInputStreamWithException;
import de.mas.wiiu.jnus.utils.StreamUtils;
import de.mas.wiiu.jnus.utils.Utils;
import lombok.extern.java.Log;

@Log
public class DefaultNUSDataProcessor implements NUSDataProcessor {
    protected final NUSDataProvider dataProvider;
    private final Optional<ContentDecryptor> decryptor;

    public DefaultNUSDataProcessor(NUSDataProvider dataProvider, Optional<ContentDecryptor> decryptor) {
        this.dataProvider = dataProvider;
        this.decryptor = decryptor;
    }

    @Override
    public InputStream readContentAsStream(Content c, long offset, long size) throws IOException {
        return dataProvider.readRawContentAsStream(c, offset, size);
    }

    @Override
    public InputStream readDecryptedContentAsStream(Content c, long offset, long size) throws IOException {
        if (!c.isEncrypted()) {
            return dataProvider.readRawContentAsStream(c, offset, size);
        }

        PipedOutputStream out = new PipedOutputStream();
        PipedInputStreamWithException in = new PipedInputStreamWithException(out, 0x10000);

        new Thread(() -> {
            try {
                readDecryptedContentToStream(out, c, offset, size);
                in.throwException(null);
            } catch (Exception e) {
                in.throwException(e);
                try {
                    out.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }).start();

        return in;
    }

    @Override
    public long readDecryptedContentToStream(OutputStream out, Content c, long offset, long size) throws IOException {
        if (!c.isEncrypted()) {
            InputStream in = dataProvider.readRawContentAsStream(c, offset, size);
            return StreamUtils.saveInputStreamToOutputStream(in, out, size);
        }

        if (!decryptor.isPresent()) {
            throw new IOException("Decryptor was null. Maybe the ticket is missing?");
        }

        if (c.isHashed()) {
            long stream_offset = (offset / 0x10000) * 0x10000;

            InputStream in = dataProvider.readRawContentAsStream(c, stream_offset, size + offset - stream_offset);

            return decryptor.get().readDecryptedContentToStreamHashed(in, out, offset, size, offset - stream_offset, dataProvider.getContentH3Hash(c).get());
        } else {
            byte[] IV = new byte[0x10];
            IV[0] = (byte) ((c.getIndex() >> 8) & 0xFF);
            IV[1] = (byte) (c.getIndex() & 0xFF);

            long streamOffset = (offset / 16) * 16;
            long streamFilesize = size;

            // if we have an offset we can't calculate the hash anymore
            // we need a new IV
            if (streamOffset > 15) {
                streamFilesize = size;

                streamOffset -= 16;
                streamFilesize += 16;

                // We need to get the current IV as soon as we get the InputStream.
                IV = null;
            } else if ((offset > 0 && offset < 16) && size < 16) {
                streamFilesize = 16;
            }

            long curStreamOffset = streamOffset;

            InputStream in = dataProvider.readRawContentAsStream(c, streamOffset, streamFilesize);
            if (IV == null) {
                // If we read with an offset > 16 we need the previous 16 bytes because they are the IV.
                // The input stream has been prepared to start 16 bytes earlier on this case.
                int toRead = 16;
                byte[] data = new byte[toRead];
                int readTotal = 0;
                while (readTotal < toRead) {
                    int res = in.read(data, readTotal, toRead - readTotal);
                    if (res < 0) {
                        StreamUtils.closeAll(in, out);
                        return -1;
                    }
                    readTotal += res;
                }
                IV = Arrays.copyOfRange(data, 0, toRead);
                curStreamOffset = streamOffset + 16;
            }
            long res = decryptor.get().readDecryptedContentToStreamNonHashed(in, out, curStreamOffset, size, offset - curStreamOffset, IV);

            return res;
        }
    }

    @Override
    public InputStream readPlainDecryptedContentAsStream(Content c, long offset, long size, boolean forceCheckHash) throws IOException {
        PipedOutputStream out = new PipedOutputStream();
        PipedInputStreamWithException in = new PipedInputStreamWithException(out, 0x10000);

        new Thread(() -> {
            try {
                readPlainDecryptedContentToStream(out, c, offset, size, forceCheckHash);
                in.throwException(null);
            } catch (Exception e) {
                in.throwException(e);
                try {
                    in.close();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        }).start();

        return in;
    }

    @Override
    public long readPlainDecryptedContentToStream(OutputStream out, Content c, long offset, long size, boolean forceCheckHash) throws IOException {
        if (c.isHashed()) {
            long payloadOffset = offset;
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

            InputStream in = readDecryptedContentAsStream(c, streamOffset, streamFilesize);

            try {
                return processHashedStream(in, out, (int) (offset / 0xFC00), size, offsetInBlock, dataProvider.getContentH3Hash(c).get());
            } catch (NoSuchAlgorithmException | CheckSumWrongException e) {
                throw new IOException(e);
            }
        } else {
            InputStream in = readDecryptedContentAsStream(c, offset, Utils.align(size, 16));

            byte[] hash = null;
            if (forceCheckHash) {
                hash = c.getSHA2Hash();
            }

            try {
                return processNonHashedStream(in, out, offset, size, hash, c.getEncryptedFileSize());
            } catch (CheckSumWrongException e) {
                throw new IOException(e);
            }
        }
    }

    private long processNonHashedStream(InputStream inputStream, OutputStream outputStream, long payloadOffset, long filesize, byte[] hash,
            long expectedSizeForHash) throws IOException, CheckSumWrongException {
        MessageDigest sha1 = null;
        MessageDigest sha1fallback = null;

        if (hash != null) {
            try {
                sha1 = MessageDigest.getInstance("SHA1");
                sha1fallback = MessageDigest.getInstance("SHA1");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }

        int BLOCKSIZE = 0x8000;

        byte[] blockBuffer = new byte[BLOCKSIZE];

        int inBlockBuffer;
        long written = 0;
        long writtenFallback = 0;

        try {
            ByteArrayBuffer overflow = new ByteArrayBuffer(BLOCKSIZE);

            // We can only decrypt multiples of 16. So we need to align it.
            long toRead = Utils.align(filesize, 16);

            do {
                int curReadSize = BLOCKSIZE;
                if (toRead < BLOCKSIZE) {
                    curReadSize = (int) toRead;
                }
                inBlockBuffer = StreamUtils.getChunkFromStream(inputStream, blockBuffer, overflow, curReadSize);
                if (inBlockBuffer <= 0) {
                    break;
                }

                byte[] output = blockBuffer;

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

                if (written >= filesize && hash == null) {
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
                byte[] expected_hash = hash;
                if (!Arrays.equals(calculated_hash1, expected_hash) && !Arrays.equals(calculated_hash2, expected_hash)) {
                    throw new CheckSumWrongException("hash checksum failed ", calculated_hash1, expected_hash);
                } else {
                    log.fine("Hash DOES match saves output stream.");
                }

            }
        } finally {
            StreamUtils.closeAll(inputStream, outputStream);
        }
        return written;
    }

    private long processHashedStream(InputStream inputStream, OutputStream outputStream, int block, long filesize, long payloadOffset, byte[] h3_hashes)
            throws IOException, NoSuchAlgorithmException, CheckSumWrongException {
        int BLOCKSIZE = 0x10000;
        int HASHBLOCKSIZE = 0xFC00;
        int HASHSIZE = BLOCKSIZE - HASHBLOCKSIZE;

        long curBlock = block;

        byte[] blockBuffer = new byte[BLOCKSIZE];
        ByteArrayBuffer overflow = new ByteArrayBuffer(BLOCKSIZE);
        long written = 0;
        int inBlockBuffer = 0;

        long writeOffset = payloadOffset;

        try {
            do {
                inBlockBuffer = StreamUtils.getChunkFromStream(inputStream, blockBuffer, overflow, BLOCKSIZE);
                if (inBlockBuffer < 0) {
                    break;
                }

                if (inBlockBuffer != BLOCKSIZE) {
                    throw new IOException("buffer was not " + BLOCKSIZE + " bytes");
                }

                byte[] hashes = null;
                byte[] output = null;

                hashes = Arrays.copyOfRange(blockBuffer, 0, HASHSIZE);
                output = Arrays.copyOfRange(blockBuffer, HASHSIZE, BLOCKSIZE);

                HashUtil.checkFileChunkHashes(hashes, h3_hashes, output, (int) curBlock);

                try {
                    long writeLength = Math.min((output.length - writeOffset), (filesize - written));
                    outputStream.write(output, (int) writeOffset, (int) writeLength);
                    written += writeLength;
                } catch (IOException e) {
                    if (e.getMessage().equals("Pipe closed")) {
                        break;
                    }
                    e.printStackTrace();
                    throw e;
                }
                writeOffset = 0;

                curBlock++;
            } while (written < filesize);
            log.finest("Decryption okay");
        } finally {
            StreamUtils.closeAll(inputStream, outputStream);
        }
        return written > 0 ? written : -1;
    }

    @Override
    public NUSDataProvider getDataProvider() {
        return dataProvider;
    }

}
