package de.mas.wiiu.jnus.interfaces;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.mas.wiiu.jnus.entities.TMD.Content;
import de.mas.wiiu.jnus.utils.StreamUtils;

public interface NUSDataProcessor {

    public NUSDataProvider getDataProvider();

    default public byte[] readContent(Content c) throws IOException {
        return readContent(c, 0, c.getEncryptedFileSizeAligned());
    }

    default public byte[] readContent(Content c, long offset, long size) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        readContentToStream(out, c, offset, size);

        return out.toByteArray();
    }

    default public InputStream readContentAsStream(Content c) throws IOException {
        return readContentAsStream(c, 0, c.getEncryptedFileSizeAligned());
    }

    public InputStream readContentAsStream(Content c, long offset, long size) throws IOException;

    default public long readContentToStream(OutputStream out, Content entry) throws IOException {
        return readContentToStream(out, entry, 0, entry.getEncryptedFileSizeAligned());
    }

    default public long readContentToStream(OutputStream out, Content entry, long offset) throws IOException {
        return readContentToStream(out, entry, offset, entry.getEncryptedFileSizeAligned());
    }

    default public long readContentToStream(OutputStream out, Content c, long offset, long size) throws IOException {
        InputStream in = readContentAsStream(c, offset, size);
        return StreamUtils.saveInputStreamToOutputStream(in, out, size);
    }

    default public byte[] readDecryptedContent(Content c) throws IOException {
        return readDecryptedContent(c, 0, c.getEncryptedFileSizeAligned());
    }

    default public byte[] readDecryptedContent(Content c, long offset, long size) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long len = readDecryptedContentToStream(out, c, offset, size);
        if (len < 0) {
            return new byte[0];
        }
        return out.toByteArray();
    }

    default public InputStream readDecryptedContentAsStream(Content c) throws IOException {
        return readDecryptedContentAsStream(c, 0, c.getEncryptedFileSizeAligned());
    }

    public InputStream readDecryptedContentAsStream(Content c, long offset, long size) throws IOException;

    default public long readDecryptedContentToStream(OutputStream out, Content c) throws IOException {
        return readDecryptedContentToStream(out, c, 0, c.getEncryptedFileSizeAligned());
    }

    default public long readDecryptedContentToStream(OutputStream out, Content c, long offset) throws IOException {
        return readDecryptedContentToStream(out, c, offset, c.getEncryptedFileSizeAligned());
    }

    default public long readDecryptedContentToStream(OutputStream out, Content c, long offset, long size) throws IOException {
        InputStream in = readDecryptedContentAsStream(c, offset, size);
        return StreamUtils.saveInputStreamToOutputStream(in, out, size);
    }

    default public byte[] readPlainDecryptedContent(Content c, boolean forceCheckHash) throws IOException {
        return readPlainDecryptedContent(c, 0, c.getEncryptedFileSizeAligned(), forceCheckHash);
    }

    default public byte[] readPlainDecryptedContent(Content c, long offset, long size, boolean forceCheckHash) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        long len = readPlainDecryptedContentToStream(out, c, offset, size, forceCheckHash);
        if (len < 0) {
            return new byte[0];
        }

        return out.toByteArray();
    }

    default public InputStream readPlainDecryptedContentAsStream(Content c, boolean forceCheckHash) throws IOException {
        return readPlainDecryptedContentAsStream(c, 0, c.getEncryptedFileSizeAligned(), forceCheckHash);
    }

    public InputStream readPlainDecryptedContentAsStream(Content c, long offset, long size, boolean forceCheckHash) throws IOException;

    default public long readPlainDecryptedContentToStream(OutputStream out, Content entry, boolean forceCheckHash) throws IOException {
        return readPlainDecryptedContentToStream(out, entry, 0, entry.getEncryptedFileSizeAligned(), forceCheckHash);
    }

    default public long readPlainDecryptedContentToStream(OutputStream out, Content entry, long offset, boolean forceCheckHash) throws IOException {
        return readPlainDecryptedContentToStream(out, entry, offset, entry.getEncryptedFileSizeAligned(), forceCheckHash);
    }

    default public long readPlainDecryptedContentToStream(OutputStream out, Content c, long offset, long size, boolean forceCheckHash) throws IOException {
        InputStream in = readPlainDecryptedContentAsStream(c, offset, size, forceCheckHash);
        return StreamUtils.saveInputStreamToOutputStream(in, out, size);
    }
}
