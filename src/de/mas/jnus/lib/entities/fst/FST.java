package de.mas.jnus.lib.entities.fst;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import de.mas.jnus.lib.entities.content.Content;
import de.mas.jnus.lib.entities.content.ContentFSTInfo;
import de.mas.jnus.lib.utils.ByteUtils;
import lombok.Getter;
/**
 * Represents the FST
 * @author Maschell
 *
 */
public final class FST {
    @Getter private final FSTEntry root = FSTEntry.getRootFSTEntry();
    
    @Getter private final int unknown;
    @Getter private final int contentCount;
    
    @Getter private final Map<Integer,ContentFSTInfo> contentFSTInfos = new HashMap<>();
   
    private FST(int unknown, int contentCount) {
        super();
        this.unknown = unknown;
        this.contentCount = contentCount;
    }
    
    /**
     * Creates a FST by the given raw byte data
     * @param fstData raw decrypted FST data
     * @param contentsMappedByIndex map of index/content
     * @return
     */
    public static FST parseFST(byte[] fstData,Map<Integer,Content> contentsMappedByIndex){
        if(!Arrays.equals(Arrays.copyOfRange(fstData, 0, 3), new byte[]{0x46,0x53,0x54})){
            throw new IllegalArgumentException("Not a FST. Maybe a wrong key?");      
        }

        int unknownValue = ByteUtils.getIntFromBytes(fstData, 0x04);
        int contentCount = ByteUtils.getIntFromBytes(fstData, 0x08);
        
        FST result = new FST(unknownValue,contentCount);
        
        int contentfst_offset = 0x20;
        int contentfst_size = 0x20*contentCount;
        
        int fst_offset = contentfst_offset+contentfst_size;
        int fileCount = ByteUtils.getIntFromBytes(fstData, fst_offset + 0x08);
        int fst_size = fileCount*0x10;
        
        int nameOff = fst_offset + fst_size;
        int nameSize = nameOff +1;
        
        //Get list with null-terminated Strings. Ends with \0\0.
        for(int i = nameOff;i<fstData.length-1;i++){
            if(fstData[i] == 0 && fstData[i+1] == 0){
                nameSize = i - nameOff-1;
            }           
        }
        
        Map<Integer,ContentFSTInfo> contentFSTInfos = result.getContentFSTInfos();
        for(int i = 0;i<contentCount;i++){
            byte contentFST[] = Arrays.copyOfRange(fstData, contentfst_offset + (i*0x20), contentfst_offset + ((i+1)*0x20));  
            contentFSTInfos.put(i,ContentFSTInfo.parseContentFST(contentFST));
        }
        
        byte fstSection[]  =  Arrays.copyOfRange(fstData, fst_offset, fst_offset + fst_size);
        byte nameSection[]  =  Arrays.copyOfRange(fstData, nameOff, nameOff + nameSize);
        
        FSTEntry root = result.getRoot();
        
        FSTService.parseFST(root,fstSection,nameSection,contentsMappedByIndex,contentFSTInfos);

        return result;
    }
}