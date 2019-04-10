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
package de.mas.wiiu.jnus.implementations.wud;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import de.mas.wiiu.jnus.utils.ByteUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

@Log
public class WUDImageCompressedInfo {
    public static final int WUX_HEADER_SIZE = 0x20;
    public static final int WUX_MAGIC_0 = 0x30585557;
    public static final int WUX_MAGIC_1 = 0x1099d02e;
    public static final int SECTOR_SIZE = 0x8000;

    @Getter private final int sectorSize;
    @Getter private final long uncompressedSize;
    @Getter private final int flags;

    @Getter @Setter private long indexTableEntryCount;
    @Getter private final long offsetIndexTable = WUX_HEADER_SIZE;
    @Getter @Setter private long offsetSectorArray;
    @Getter @Setter private long indexTableSize;

    private final boolean valid;

    @Getter private Map<Integer, Long> indexTable = new HashMap<>();

    public WUDImageCompressedInfo(byte[] headData) {
        if (headData.length < WUX_HEADER_SIZE) {
            log.info("WUX header length wrong");
            System.exit(1);
        }
        int magic0 = ByteUtils.getIntFromBytes(headData, 0x00, ByteOrder.LITTLE_ENDIAN);
        int magic1 = ByteUtils.getIntFromBytes(headData, 0x04, ByteOrder.LITTLE_ENDIAN);
        if (magic0 == WUX_MAGIC_0 && magic1 == WUX_MAGIC_1) {
            valid = true;
        } else {
            valid = false;
        }
        this.sectorSize = ByteUtils.getIntFromBytes(headData, 0x08, ByteOrder.LITTLE_ENDIAN);
        this.flags = ByteUtils.getIntFromBytes(headData, 0x0C, ByteOrder.LITTLE_ENDIAN);
        this.uncompressedSize = ByteUtils.getLongFromBytes(headData, 0x10, ByteOrder.LITTLE_ENDIAN);

        if (valid) {
            calculateOffsets();
        }
    }

    public static WUDImageCompressedInfo getDefaultCompressedInfo() {
        return new WUDImageCompressedInfo(SECTOR_SIZE, 0, WUDImage.WUD_FILESIZE);
    }

    public WUDImageCompressedInfo(int sectorSize, int flags, long uncompressedSize) {
        this.sectorSize = sectorSize;
        this.flags = flags;
        this.uncompressedSize = uncompressedSize;
        valid = true;
        calculateOffsets();
    }

    private void calculateOffsets() {
        long indexTableEntryCount = (getUncompressedSize() + getSectorSize() - 1) / getSectorSize();
        setIndexTableEntryCount(indexTableEntryCount);
        long offsetSectorArray = (getOffsetIndexTable() + ((long) getIndexTableEntryCount() * 0x04L));
        // align to SECTOR_SIZE
        offsetSectorArray = (offsetSectorArray + (long) (getSectorSize() - 1));
        offsetSectorArray = offsetSectorArray - (offsetSectorArray % (long) getSectorSize());
        setOffsetSectorArray(offsetSectorArray);
        // read index table
        setIndexTableSize(0x04 * getIndexTableEntryCount());
    }

    public boolean isWUX() {
        return valid;
    }

    public long getSectorIndex(int sectorIndex) {
        return getIndexTable().get(sectorIndex);
    }

    public void setIndexTable(Map<Integer, Long> indexTable) {
        this.indexTable = indexTable;
    }

    public byte[] getHeaderAsBytes() {
        ByteBuffer result = ByteBuffer.allocate(WUX_HEADER_SIZE);
        result.order(ByteOrder.LITTLE_ENDIAN);
        result.putInt(WUX_MAGIC_0);
        result.putInt(WUX_MAGIC_1);
        result.putInt(getSectorSize());
        result.putInt(getFlags());
        result.putLong(getUncompressedSize());
        return result.array();
    }
}
