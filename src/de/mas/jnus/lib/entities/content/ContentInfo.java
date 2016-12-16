package de.mas.jnus.lib.entities.content;

import java.nio.ByteBuffer;
import java.util.Arrays;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
/**
 * Represents a Object from the TMD before the actual Content Section.
 * @author Maschell
 *
 */
public class ContentInfo{
    @Getter private final short indexOffset;
    @Getter private final short commandCount;
    @Getter private final byte[] SHA2Hash;
        
    public ContentInfo() {
        this((short) 0);
    }
 
    public ContentInfo(short contentCount) {
        this((short) 0,contentCount);
    }
    public ContentInfo(short indexOffset,short commandCount) {
        this(indexOffset,commandCount,null);
    }
    public ContentInfo(short indexOffset,short commandCount,byte[] SHA2Hash) {
        this.indexOffset = indexOffset;
        this.commandCount = commandCount;
        this.SHA2Hash = SHA2Hash;
    }
    
    /**
     * Creates a new ContentInfo object given be the raw byte data
     * @param input 0x24 byte of data from the TMD (starting at 0x208)
     * @return ContentFSTInfo object
     */
    public static ContentInfo parseContentInfo(byte[] input){
        if(input == null || input.length != 0x24){
            System.out.println("Error: invalid ContentInfo byte[] input");
            return null;
        }
        
        ByteBuffer buffer = ByteBuffer.allocate(input.length);
        buffer.put(input);
        buffer.position(0);
        short indexOffset = buffer.getShort(0x00);
        short commandCount = buffer.getShort(0x02);

        byte[] sha2hash =  new byte[0x20];
        buffer.position(0x04);
        buffer.get(sha2hash, 0x00, 0x20);
        
        return new ContentInfo(indexOffset, commandCount, sha2hash);        
    }
  
    @Override
    public String toString() {
        return "ContentInfo [indexOffset=" + indexOffset + ", commandCount=" + commandCount + ", SHA2Hash=" + Arrays.toString(SHA2Hash) + "]";
    }
}
