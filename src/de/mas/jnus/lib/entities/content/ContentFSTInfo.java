package de.mas.jnus.lib.entities.content;

import java.nio.ByteBuffer;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode
/**
 * Representation on an Object of the first section
 * of an FST. 
 * @author Maschell
 *
 */
public class ContentFSTInfo {
    @Getter @Setter private long offsetSector;
    @Getter @Setter private long sizeSector;
    @Getter @Setter private long ownerTitleID;
    @Getter @Setter private int groupID;
    @Getter @Setter private byte unkown;
    
    private static int SECTOR_SIZE = 0x8000;
    
    private ContentFSTInfo(){
        
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
        ContentFSTInfo cFSTInfo = new ContentFSTInfo();
        ByteBuffer buffer = ByteBuffer.allocate(input.length);
        buffer.put(input);
        
        buffer.position(0);
        int offset = buffer.getInt();
        int size = buffer.getInt();
        long ownerTitleID = buffer.getLong();
        int groupID = buffer.getInt();
        byte unkown = buffer.get();
        
        cFSTInfo.setOffsetSector(offset);
        cFSTInfo.setSizeSector(size);
        cFSTInfo.setOwnerTitleID(ownerTitleID);
        cFSTInfo.setGroupID(groupID);
        cFSTInfo.setUnkown(unkown);
      
        return cFSTInfo;       
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
