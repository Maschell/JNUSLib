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
import java.util.Optional;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.java.Log;

@EqualsAndHashCode
/**
 * Represents a Object from the TMD before the actual Content Section.
 * 
 * @author Maschell
 *
 */
@Log
public class ContentInfo {
    public static final int CONTENT_INFO_SIZE = 0x24;

    @Getter private final short indexOffset;
    @Getter private final short commandCount;
    @Getter private final Optional<byte[]> SHA2Hash;

    public ContentInfo() {
        this((short) 0);
    }

    public ContentInfo(short contentCount) {
        this((short) 0, contentCount);
    }

    public ContentInfo(short indexOffset, short commandCount) {
        this(indexOffset, commandCount, null);
    }

    public ContentInfo(short indexOffset, short commandCount, byte[] SHA2Hash) {
        this.indexOffset = indexOffset;
        this.commandCount = commandCount;
        if (SHA2Hash == null) {
            this.SHA2Hash = Optional.empty();
        } else {
            this.SHA2Hash = Optional.of(SHA2Hash);
        }
    }

    /**
     * Creates a new ContentInfo object given be the raw byte data
     * 
     * @param input
     *            0x24 byte of data from the TMD (starting at 0x208)
     * @return ContentFSTInfo object
     * @throws ParseException
     */
    public static ContentInfo parseContentInfo(byte[] input) throws ParseException {
        if (input == null || input.length != CONTENT_INFO_SIZE) {
            log.info("Error: invalid ContentInfo byte[] input");
            throw new ParseException("Error: invalid ContentInfo byte[] input", 0);
        }

        ByteBuffer buffer = ByteBuffer.allocate(input.length);
        buffer.put(input);
        buffer.position(0);
        short indexOffset = buffer.getShort(0x00);
        short commandCount = buffer.getShort(0x02);

        byte[] sha2hash = new byte[0x20];
        buffer.position(0x04);
        buffer.get(sha2hash, 0x00, 0x20);

        return new ContentInfo(indexOffset, commandCount, sha2hash);
    }

    @Override
    public String toString() {
        return "ContentInfo [indexOffset=" + indexOffset + ", commandCount=" + commandCount + ", SHA2Hash=" + SHA2Hash + "]";
    }
}
