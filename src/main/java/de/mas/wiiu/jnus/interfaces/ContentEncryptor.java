package de.mas.wiiu.jnus.interfaces;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.mas.wiiu.jnus.utils.IVCache;

public interface ContentEncryptor {

    long readEncryptedContentToStreamHashed(InputStream in, OutputStream out, long offset, long size, long payloadOffset) throws IOException;

    long readEncryptedContentToStreamNonHashed(InputStream in, OutputStream out, long offset, long size, long payloadOffset, byte[] IV, IVCache ivcache)
            throws IOException;

}
