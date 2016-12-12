package de.mas.jnus.lib.implementations.wud;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import de.mas.jnus.lib.utils.ByteUtils;
import lombok.Getter;
import lombok.Setter;

public class WUDImageCompressedInfo {
    public static final int WUX_HEADER_SIZE = 0x20;
    public static final int WUX_MAGIC_0 = 0x30585557;
    public static final int WUX_MAGIC_1 = 0x1099d02e;
    public static final int SECTOR_SIZE = 0x8000;
    
    @Getter @Setter private int magic0;
    @Getter @Setter private int magic1;
    @Getter @Setter private int sectorSize;
    @Getter @Setter private long uncompressedSize;
    @Getter @Setter private int flags;
    
    @Getter @Setter private long indexTableEntryCount = 0;
    @Getter @Setter private long offsetIndexTable = 0;
    @Getter @Setter private long offsetSectorArray = 0;
    @Getter @Setter private long indexTableSize = 0;
    
    @Getter private Map<Integer,Long> indexTable = new HashMap<>();
    
    public WUDImageCompressedInfo(byte[] headData){
        if(headData.length < WUX_HEADER_SIZE){
            System.out.println("WUX header length wrong");
            System.exit(1);
        }
        setMagic0(ByteUtils.getIntFromBytes(headData, 0x00,ByteOrder.LITTLE_ENDIAN));
        setMagic1(ByteUtils.getIntFromBytes(headData, 0x04,ByteOrder.LITTLE_ENDIAN));
        setSectorSize(ByteUtils.getIntFromBytes(headData, 0x08,ByteOrder.LITTLE_ENDIAN));
        setFlags(ByteUtils.getIntFromBytes(headData, 0x0C,ByteOrder.LITTLE_ENDIAN));
        setUncompressedSize(ByteUtils.getLongFromBytes(headData, 0x10,ByteOrder.LITTLE_ENDIAN));
                
        calculateOffsets();
    }
    
    public static WUDImageCompressedInfo getDefaultCompressedInfo(){
        return new WUDImageCompressedInfo(SECTOR_SIZE, 0, WUDImage.WUD_FILESIZE);
    }
    
    public WUDImageCompressedInfo(int sectorSize,int flags, long uncompressedSize) {
        setMagic0(WUX_MAGIC_0);
        setMagic1(WUX_MAGIC_1);
        setSectorSize(sectorSize);
        setFlags(flags);
        setUncompressedSize(uncompressedSize);
    }

    private void calculateOffsets() {
        long indexTableEntryCount = (getUncompressedSize()+ getSectorSize()-1) / getSectorSize();
        setIndexTableEntryCount(indexTableEntryCount);
        setOffsetIndexTable(0x20); 
        long offsetSectorArray = (getOffsetIndexTable() + ((long)getIndexTableEntryCount() * 0x04L));
        // align to SECTOR_SIZE
        offsetSectorArray = (offsetSectorArray + (long)(getSectorSize()-1));
        offsetSectorArray = offsetSectorArray - (offsetSectorArray%(long)getSectorSize());
        setOffsetSectorArray(offsetSectorArray);
        // read index table
        setIndexTableSize(0x04 * getIndexTableEntryCount());
    }

    public boolean isWUX() {
        return (getMagic0() == WUX_MAGIC_0 && getMagic1() == WUX_MAGIC_1);
    }

    @Override
    public String toString() {
        return "WUDImageCompressedInfo [magic0=" + String.format("0x%08X", magic0) + ", magic1=" + String.format("0x%08X", magic1) + ", sectorSize=" + String.format("0x%08X", sectorSize)
                + ", uncompressedSize=" + String.format("0x%016X", uncompressedSize) + ", flags=" + String.format("0x%08X", flags) + ", indexTableEntryCount="
                + indexTableEntryCount + ", offsetIndexTable=" + offsetIndexTable + ", offsetSectorArray="
                + offsetSectorArray + ", indexTableSize=" + indexTableSize + "]";
    }

    public long getSectorIndex(int sectorIndex) {
        return getIndexTable().get(sectorIndex);
    }

    public void setIndexTable(Map<Integer,Long> indexTable) {
        this.indexTable = indexTable;
    }

    public byte[] getHeaderAsBytes() {
        ByteBuffer result = ByteBuffer.allocate(WUX_HEADER_SIZE);
        result.order(ByteOrder.LITTLE_ENDIAN);
        result.putInt(getMagic0());
        result.putInt(getMagic1());
        result.putInt(getSectorSize());
        result.putInt(getFlags());
        result.putLong(getUncompressedSize());
        return result.array();
    }
}
