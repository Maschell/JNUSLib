package de.mas.wiiu.jnus.utils.cryptography;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchProviderException;
import java.util.Arrays;

import de.mas.wiiu.jnus.entities.Ticket;
import de.mas.wiiu.jnus.interfaces.ContentEncryptor;
import de.mas.wiiu.jnus.utils.ByteArrayBuffer;
import de.mas.wiiu.jnus.utils.IVCache;
import de.mas.wiiu.jnus.utils.StreamUtils;
import lombok.Synchronized;

public class NUSEncryption extends AESEncryption implements ContentEncryptor {
    public NUSEncryption(byte[] AESKey, byte[] IV) throws NoSuchProviderException {
        super(AESKey, IV);
    }

    public NUSEncryption(Ticket ticket) throws NoSuchProviderException {
        this(ticket.getDecryptedKey(), ticket.getIV());
    }

    @Synchronized
    private byte[] encryptFileChunk(byte[] blockBuffer, int BLOCKSIZE, byte[] IV) {
        return encryptFileChunk(blockBuffer, 0, BLOCKSIZE, IV);
    }

    @Synchronized
    private byte[] encryptFileChunk(byte[] blockBuffer, int offset, int BLOCKSIZE, byte[] IV) {
        if (IV != null) {
            setIV(IV);
            init();
        }
        return encrypt(blockBuffer, offset, BLOCKSIZE);
    }

    @Override
    public long readEncryptedContentToStreamHashed(InputStream in, OutputStream out, long offset, long size, long payloadOffset) throws IOException {
        int BLOCKSIZE = 0x10000;

        int HASHBLOCKSIZE = 0x400;
        int HASHEDBLOCKSIZE = 0xFC00;

        int buffer_size = BLOCKSIZE;
        byte[] decryptedBlockBuffer = new byte[buffer_size];
        ByteArrayBuffer overflowbuffer = new ByteArrayBuffer(buffer_size);
        int block = (int) (offset / 0x10000);
        int inBlockBuffer = 0;
        long read = 0;
        long written = 0;

        try {
            do {
                inBlockBuffer = StreamUtils.getChunkFromStream(in, decryptedBlockBuffer, overflowbuffer, BLOCKSIZE);
                read += inBlockBuffer;
                if (read - offset < payloadOffset) {
                    continue;
                }
                if (inBlockBuffer != buffer_size) {
                    break;
                }

                long curOffset = Math.max(0, payloadOffset - offset - read);

                byte[] IV = new byte[16];
                if (curOffset < HASHBLOCKSIZE) {
                    byte[] encryptedhashes = encryptFileChunk(Arrays.copyOfRange(decryptedBlockBuffer, 0, HASHBLOCKSIZE), HASHBLOCKSIZE, IV);

                    long writeLength = Math.min((encryptedhashes.length - curOffset), (size - written));

                    out.write(encryptedhashes, (int) curOffset, (int) writeLength);
                    written += writeLength;
                } else {
                    curOffset = curOffset > HASHBLOCKSIZE ? curOffset - HASHBLOCKSIZE : 0;
                }
                if (curOffset < HASHEDBLOCKSIZE) {
                    int iv_start = (block % 16) * 20;
                    IV = Arrays.copyOfRange(decryptedBlockBuffer, iv_start, iv_start + 16);

                    byte[] encryptedContent = encryptFileChunk(Arrays.copyOfRange(decryptedBlockBuffer, HASHBLOCKSIZE, HASHEDBLOCKSIZE + HASHBLOCKSIZE),
                            HASHEDBLOCKSIZE, IV);

                    long writeLength = Math.min((encryptedContent.length - curOffset), (size - written));
                    out.write(encryptedContent, (int) curOffset, (int) writeLength);
                    written += writeLength;
                }

                block++;
            } while (inBlockBuffer == buffer_size);
        } finally {
            StreamUtils.closeAll(in, out);
        }
        return written > 0 ? written : -1;
    }

    @Override
    public long readEncryptedContentToStreamNonHashed(InputStream in, OutputStream out, long offset, long size, long payloadOffset, byte[] IV, IVCache ivcache)
            throws IOException {
        int BLOCKSIZE = 0x08000;

        int buffer_size = BLOCKSIZE;
        byte[] decryptedBlockBuffer = new byte[buffer_size];
        ByteArrayBuffer overflowbuffer = new ByteArrayBuffer(buffer_size);
        int inBlockBuffer = 0;

        setIV(IV);
        init();

        long read = 0;
        long written = 0;

        long curPos = offset;

        try {
            do {
                int curReadLength = (int) (curPos % buffer_size);
                if (curReadLength == 0) {
                    curReadLength = buffer_size;
                }
                inBlockBuffer = StreamUtils.getChunkFromStream(in, decryptedBlockBuffer, overflowbuffer, curReadLength);
                if (inBlockBuffer < 0) {
                    break;
                }
                curPos += inBlockBuffer;
                read += inBlockBuffer;

                byte[] output = encrypt(decryptedBlockBuffer, 0, inBlockBuffer);

                byte[] curIV = Arrays.copyOfRange(output, output.length - 16, output.length);
                ivcache.addForOffset(curPos, curIV);

                setIV(Arrays.copyOfRange(output, BLOCKSIZE - 16, BLOCKSIZE));
                init();

                if (read < payloadOffset) {
                    continue;
                }

                long writeOffset = Math.max(0, payloadOffset - offset - read);
                long writeLength = Math.min((output.length - writeOffset), (size - written));

                out.write(output, (int) writeOffset, (int) writeLength);
                written += writeLength;
            } while (written < size);
        } finally {
            StreamUtils.closeAll(in, out);
        }
        return written > 0 ? written : -1;
    }
}
