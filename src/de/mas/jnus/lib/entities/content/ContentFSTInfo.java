package de.mas.jnus.lib.entities.content;

import java.nio.ByteBuffer;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
/**
 * Representation on an Object of the first section
 * of an FST. 
 * @author Maschell
 *
 */
public final class ContentFSTInfo {
    @Getter private final long offsetSector;
    @Getter private final long sizeSector;
    @Getter private final long ownerTitleID;
    @Getter private final int groupID;
    @Getter private final byte unkown;
    
    private static int SECTOR_SIZE = 0x8000;
    
    private ContentFSTInfo(ContentFSTInfoParam param){
        this.offsetSector = param.getOffsetSector();
        this.sizeSector = param.getSizeSector();
        this.ownerTitleID = param.getOwnerTitleID();
        this.groupID = param.getGroupID();
        this.unkown = param.getUnkown();
    }

    /**
     * Creates a new ContentFSTInfo object given be the raw byte data
     * @param input 0x20 byte of data from the FST (starting at 0x20)
     * @return ContentFSTInfo object
     */
    public static ContentFSTInfo parseContentFST(byte[] input) {
        if(input == null ||input.length != 0x20){
            System.out.println("Error: invalid ContentFSTInfo byte[] input");
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
     * @return offset of the content in the partition in bytes
     */
    public long getOffset() {
        long result = (getOffsetSector() * SECTOR_SIZE) - SECTOR_SIZE;
        if(result < 0){
            return 0;
        }
        return result;
    }

    /**
     * Returns the size in bytes, not in sectors
     * @return size in bytes
     */
    public int getSize() {
        return (int) (getSizeSector() * SECTOR_SIZE);
    }
    
    @Override
    public String toString() {
        return "ContentFSTInfo [offset=" + String.format("%08X", offsetSector) + ", size=" + String.format("%08X", sizeSector) + ", ownerTitleID=" + String.format("%016X", ownerTitleID) + ", groupID="
                + String.format("%08X", groupID) + ", unkown=" + unkown + "]";
    }
}
