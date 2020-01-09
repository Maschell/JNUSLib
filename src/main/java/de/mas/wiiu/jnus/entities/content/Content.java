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
package de.mas.wiiu.jnus.entities.content;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Arrays;

import de.mas.wiiu.jnus.Settings;
import de.mas.wiiu.jnus.utils.Utils;
import lombok.Data;
import lombok.Getter;
import lombok.extern.java.Log;

/**
 * Represents a Content
 * 
 * @author Maschell
 *
 */
@Log
public class Content implements Comparable<Content> {
    public static final short CONTENT_FLAG_UNKWN1 = 0x4000;
    public static final short CONTENT_HASHED = 0x0002;
    public static final short CONTENT_ENCRYPTED = 0x0001;
    public static final int CONTENT_SIZE = 0x30;

    @Getter private final int ID;
    @Getter private final short index;
    @Getter private final short type;

    @Getter private final long encryptedFileSize;
    @Getter private final byte[] SHA2Hash;

    private Content(ContentParam param) {
        this.ID = param.getID();
        this.index = param.getIndex();
        this.type = param.getType();
        this.encryptedFileSize = param.getEncryptedFileSize();
        this.SHA2Hash = param.getSHA2Hash();
    }

    /**
     * Creates a new Content object given be the raw byte data
     * 
     * @param input
     *            0x30 byte of data from the TMD (starting at 0xB04)
     * @return content object
     * @throws ParseException
     */
    public static Content parseContent(byte[] input) throws ParseException {
        if (input == null || input.length != CONTENT_SIZE) {
            log.info("Error: invalid Content byte[] input");
            throw new ParseException("Error: invalid Content byte[] input", 0);
        }
        ByteBuffer buffer = ByteBuffer.allocate(input.length);
        buffer.put(input);
        buffer.position(0);

        int ID = buffer.getInt(0x00);
        short index = buffer.getShort(0x04);
        short type = buffer.getShort(0x06);
        long encryptedFileSize = buffer.getLong(0x08);
        buffer.position(0x10);
        byte[] hash = new byte[0x14];
        buffer.get(hash, 0x00, 0x14);

        ContentParam param = new ContentParam();
        param.setID(ID);
        param.setIndex(index);
        param.setType(type);
        param.setEncryptedFileSize(encryptedFileSize);
        param.setSHA2Hash(hash);

        return new Content(param);
    }

    /**
     * Returns if the content is hashed
     * 
     * @return true if hashed
     */
    public boolean isHashed() {
        return (type & CONTENT_HASHED) == CONTENT_HASHED;
    }

    /**
     * Returns if the content is encrypted
     * 
     * @return true if encrypted
     */
    public boolean isEncrypted() {
        return (type & CONTENT_ENCRYPTED) == CONTENT_ENCRYPTED;
    }

    public boolean isUNKNWNFlag1Set() {
        return (type & CONTENT_FLAG_UNKWN1) == CONTENT_FLAG_UNKWN1;
    }

    /**
     * Return the filename of the encrypted content. It's the ID as hex with an extension For example: 00000000.app
     * 
     * @return filename of the encrypted content
     */
    public String getFilename() {
        return String.format("%08X%s", getID(), Settings.ENCRYPTED_CONTENT_EXTENTION);
    }

    /**
     * Returns the size of the decrypted content.
     * 
     * @return size of the decrypted content
     */
    public long getDecryptedFileSize() {
        if (isHashed()) {
            return getEncryptedFileSize() / 0x10000 * 0xFC00;
        } else {
            return getEncryptedFileSize();
        }
    }

    /**
     * Return the filename of the decrypted content. It's the ID as hex with an extension For example: 00000000.dec
     * 
     * @return filename of the decrypted content
     */
    public String getFilenameDecrypted() {
        return String.format("%08X%s", getID(), Settings.DECRYPTED_CONTENT_EXTENTION);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ID;
        result = prime * result + Arrays.hashCode(SHA2Hash);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Content other = (Content) obj;
        if (ID != other.ID) return false;
        return Arrays.equals(SHA2Hash, other.SHA2Hash);
    }

    public long getEncryptedFileSizeAligned() {
        return Utils.align(encryptedFileSize, 16);
    }

    @Override
    public String toString() {
        return "Content [ID=" + Integer.toHexString(ID) + ", index=" + Integer.toHexString(index) + ", type=" + String.format("%04X", type)
                + ", encryptedFileSize=" + encryptedFileSize + ", SHA2Hash=" + Utils.ByteArrayToString(SHA2Hash) + "]";
    }

    @Data
    private static class ContentParam {
        private int ID;
        private short index;
        private short type;

        private long encryptedFileSize;
        private byte[] SHA2Hash;

        private ContentFSTInfo contentFSTInfo;
    }

    @Override
    public int compareTo(Content o) {
        return Integer.compare(this.ID, o.ID);
    }

}