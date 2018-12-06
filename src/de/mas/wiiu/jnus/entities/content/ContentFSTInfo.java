/****************************************************************************
 * Copyright (C) 2016-2018 Maschell
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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.java.Log;

@EqualsAndHashCode
/**
 * Representation on an Object of the first section of an FST.
 * 
 * @author Maschell
 *
 */
@Log
public final class ContentFSTInfo {
    @Getter private final long offsetSector;
    @Getter private final long sizeSector;
    @Getter private final long ownerTitleID;
    @Getter private final int groupID;
    @Getter private final byte unkown;

    private static int SECTOR_SIZE = 0x8000;

    private ContentFSTInfo(ContentFSTInfoParam param) {
        this.offsetSector = param.getOffsetSector();
        this.sizeSector = param.getSizeSector();
        this.ownerTitleID = param.getOwnerTitleID();
        this.groupID = param.getGroupID();
        this.unkown = param.getUnkown();
    }

    /**
     * Creates a new ContentFSTInfo object given be the raw byte data
     * 
     * @param input
     *            0x20 byte of data from the FST (starting at 0x20)
     * @return ContentFSTInfo object
     */
    public static ContentFSTInfo parseContentFST(byte[] input) {
        if (input == null || input.length != 0x20) {
            log.info("Error: invalid ContentFSTInfo byte[] input");
            return null;
        }
        ContentFSTInfoParam param = new ContentFSTInfoParam();
        ByteBuffer buffer = ByteBuffer.allocate(input.length);
        buffer.put(input);

        buffer.position(0);
        int offset = buffer.getInt();
        int size = buffer.getInt();
        long ownerTitleID = buffer.getLong();
        int groupID = buffer.getInt();
        byte unkown = buffer.get();

        param.setOffsetSector(offset);
        param.setSizeSector(size);
        param.setOwnerTitleID(ownerTitleID);
        param.setGroupID(groupID);
        param.setUnkown(unkown);

        return new ContentFSTInfo(param);
    }

    /**
     * Returns the offset of of the Content in the partition
     * 
     * @return offset of the content in the partition in bytes
     */
    public long getOffset() {
        long result = (getOffsetSector() * SECTOR_SIZE) - SECTOR_SIZE;
        if (result < 0) {
            return 0;
        }
        return result;
    }

    /**
     * Returns the size in bytes, not in sectors
     * 
     * @return size in bytes
     */
    public int getSize() {
        return (int) (getSizeSector() * SECTOR_SIZE);
    }

    @Override
    public String toString() {
        return "ContentFSTInfo [offset=" + String.format("%08X", offsetSector) + ", size=" + String.format("%08X", sizeSector) + ", ownerTitleID="
                + String.format("%016X", ownerTitleID) + ", groupID=" + String.format("%08X", groupID) + ", unkown=" + unkown + "]";
    }

    @Data
    private static class ContentFSTInfoParam {
        private long offsetSector;
        private long sizeSector;
        private long ownerTitleID;
        private int groupID;
        private byte unkown;
    }

}
