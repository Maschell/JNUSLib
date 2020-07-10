package de.mas.wiiu.jnus.interfaces;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ContentDecryptor {

    /**
     * 
     * @param in InputStream of the Encrypted Data with hashed
     * @param out OutputStream of the decrypted data with hashes 
     * @param offset absolute offset in this Content stream
     * @param size size of the payload that will be written to the outputstream
     * @param payloadOffset relative offset to the start of the inputstream
     * @param h3_hashes level 3 hashes of the content file 
     * @return 
     * @throws IOException
     */
    long readDecryptedContentToStreamHashed(InputStream in, OutputStream out, long offset, long size, long payloadOffset, byte[] h3_hashes) throws IOException;

    long readDecryptedContentToStreamNonHashed(InputStream in, OutputStream out, long offset, long size, long payloadOffset, byte[] IV) throws IOException;

}
