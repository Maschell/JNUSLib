package de.mas.jnus.lib.entities;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import de.mas.jnus.lib.entities.content.Content;
import de.mas.jnus.lib.entities.content.ContentInfo;
import lombok.Getter;
import lombok.Setter;

public class TMD {
    @Getter @Setter private int             signatureType;                                  // 0x000
    @Getter @Setter private byte[]          signature           =   new byte[0x100];        // 0x004
    @Getter @Setter private byte[]          issuer              =   new byte[0x40];         // 0x140
    @Getter @Setter private byte            version;                                        // 0x180
    @Getter @Setter private byte            CACRLVersion;                                   // 0x181
    @Getter @Setter private byte            signerCRLVersion;                               // 0x182
    @Getter @Setter private long            systemVersion;                                  // 0x184
    @Getter @Setter private long            titleID;                                        // 0x18C    
    @Getter @Setter private int             titleType;                                      // 0x194    
    @Getter @Setter private short           groupID;                                        // 0x198 
    @Getter @Setter private byte[]          reserved            =   new byte[62];           // 0x19A    
    @Getter @Setter private int             accessRights;                                   // 0x1D8    
    @Getter @Setter private short           titleVersion;                                   // 0x1DC 
    @Getter @Setter private short           contentCount;                                   // 0x1DE 
    @Getter @Setter private short           bootIndex;                                      // 0x1E0    
    @Getter @Setter private byte[]          SHA2                =   new byte[0x20];         // 0x1E4
    @Getter @Setter private ContentInfo[]   contentInfos        =   new ContentInfo[0x40];  
    Map<Integer,Content>   contentToIndex = new HashMap<>();
    Map<Integer,Content>   contentToID = new HashMap<>();
    
    @Getter @Setter private byte[] rawTMD = new byte[0];
    private TMD(){
        
    }
    
    public static TMD parseTMD(File tmd) throws IOException {
        if(tmd == null || !tmd.exists()){
            System.out.println("TMD input file null or doesn't exist.");
            return null;
        }
        return parseTMD(Files.readAllBytes(tmd.toPath()));
    }

    public static TMD parseTMD(byte[] input) {
        
        TMD result = new TMD();
        result.setRawTMD(Arrays.copyOf(input,input.length));
        byte[] signature = new byte[0x100];
        byte[] issuer = new byte[0x40];
        byte[] reserved = new byte[62];
        byte[] SHA2 = new byte[0x20];
        
        ContentInfo[] contentInfos = result.getContentInfos();
         
        ByteBuffer buffer = ByteBuffer.allocate(input.length);
        buffer.put(input);
        
        //Get Signature
        buffer.position(0x00);
        int signatureType = buffer.getInt();       
        buffer.get(signature, 0, 0x100);
        
        //Get Issuer
        buffer.position(0x140);
        buffer.get(issuer, 0, 0x40);

        //Get CACRLVersion and signerCRLVersion
        buffer.position(0x180);
        byte version = buffer.get();
        byte CACRLVersion = buffer.get();
        byte signerCRLVersion = buffer.get();
        
        //Get title information
        buffer.position(0x184);
        long systemVersion = buffer.getLong();
        long titleID = buffer.getLong();
        int titleType = buffer.getInt();
        short groupID = buffer.getShort();
        buffer.position(0x19A);
        
        //Get title information
        buffer.get(reserved, 0, 62);
        
        //Get accessRights,titleVersion,contentCount,bootIndex
        buffer.position(0x1D8);
        int accessRights = buffer.getInt();
        short titleVersion = buffer.getShort();
        short contentCount = buffer.getShort();
        short bootIndex = buffer.getShort();
        
        //Get hash
        buffer.position(0x1E4);
        buffer.get(SHA2, 0, 0x20);
        
        //Get contentInfos
        buffer.position(0x204);
        for(int i =0;i<64;i++){
            byte[] contentInfo = new byte[0x24];
            buffer.get(contentInfo, 0, 0x24);
            contentInfos[i] = ContentInfo.parseContentInfo(contentInfo);
        }
        
        //Get Contents
        for(int i =0;i<contentCount;i++){
            buffer.position(0xB04+(0x30*i));
            byte[] content = new byte[0x30];
            buffer.get(content, 0, 0x30);
            Content c = Content.parseContent(content);
            result.setContentToIndex(c.getIndex(),c);
            result.setContentToID(c.getID(), c);
        }
             
        result.setSignatureType(signatureType);
        result.setSignature(signature);
        result.setVersion(version);
        result.setCACRLVersion(CACRLVersion);
        result.setSignerCRLVersion(signerCRLVersion);
        result.setSystemVersion(systemVersion);
        result.setTitleID(titleID);
        result.setTitleType(titleType);
        result.setGroupID(groupID);
        result.setAccessRights(accessRights);
        result.setTitleVersion(titleVersion);
        result.setContentCount(contentCount);
        result.setBootIndex(bootIndex);
        result.setSHA2(SHA2);
        result.setContentInfos(contentInfos);
        
        return result;        
    }
    
    public Content getContentByIndex(int index) {
        return contentToIndex.get(index);
    }
    
    private void setContentToIndex(int index,Content content) {
        contentToIndex.put(index, content);
    }
    
    public Content getContentByID(int id) {
        return contentToID.get(id);
    }
    
    private void setContentToID(int id,Content content) {
        contentToID.put(id, content);
    }
    
    /**
     * Returns all contents mapped by index
     * @return Map of Content, index/content pairs
     */
    public Map<Integer, Content> getAllContents() {
        return contentToIndex;
    }

    public void printContents() {
       for(Content c: contentToIndex.values()){
           System.out.println(c);
       }
    }
}
