package de.mas.jnus.lib.entities;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import de.mas.jnus.lib.entities.content.Content;
import de.mas.jnus.lib.entities.content.ContentInfo;
import lombok.Getter;

public final class TMD {
    @Getter private final int             signatureType;                                  // 0x000
    @Getter private final byte[]          signature;                                      // 0x004
    @Getter private final byte[]          issuer;                                         // 0x140
    @Getter private final byte            version;                                        // 0x180
    @Getter private final byte            CACRLVersion;                                   // 0x181
    @Getter private final byte            signerCRLVersion;                               // 0x182
    @Getter private final long            systemVersion;                                  // 0x184
    @Getter private final long            titleID;                                        // 0x18C    
    @Getter private final int             titleType;                                      // 0x194    
    @Getter private final short           groupID;                                        // 0x198 
    @Getter private final byte[]          reserved;                                       // 0x19A    
    @Getter private final int             accessRights;                                   // 0x1D8    
    @Getter private final short           titleVersion;                                   // 0x1DC 
    @Getter private final short           contentCount;                                   // 0x1DE 
    @Getter private final short           bootIndex;                                      // 0x1E0    
    @Getter private final byte[]          SHA2;                                           // 0x1E4
    @Getter private final ContentInfo[]   contentInfos;  
    private final Map<Integer,Content>   contentToIndex = new HashMap<>();
    private final Map<Integer,Content>   contentToID = new HashMap<>();

    private TMD(TMDParam param) {
        super();
        this.signatureType = param.getSignatureType();
        this.signature = param.getSignature();
        this.issuer = param.getIssuer();
        this.version = param.getVersion();
        this.CACRLVersion = param.getCACRLVersion();
        this.signerCRLVersion = param.getSignerCRLVersion();
        this.systemVersion = param.getSystemVersion();
        this.titleID = param.getTitleID();
        this.titleType = param.getTitleType();
        this.groupID = param.getGroupID();
        this.reserved = param.getReserved();
        this.accessRights = param.getAccessRights();
        this.titleVersion = param.getTitleVersion();
        this.contentCount = param.getContentCount();
        this.bootIndex = param.getBootIndex();
        this.SHA2 = param.getSHA2();
        this.contentInfos = param.getContentInfos();
    }
    
    public static TMD parseTMD(File tmd) throws IOException {
        if(tmd == null || !tmd.exists()){
            System.out.println("TMD input file null or doesn't exist.");
            return null;
        }
        return parseTMD(Files.readAllBytes(tmd.toPath()));
    }

    public static TMD parseTMD(byte[] input) {
        byte[] signature = new byte[0x100];
        byte[] issuer = new byte[0x40];
        byte[] reserved = new byte[62];
        byte[] SHA2 = new byte[0x20];
        
        ContentInfo[] contentInfos = new ContentInfo[0x40];
         
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
       
        TMDParam param = new TMDParam();
        param.setSignatureType(signatureType);
        param.setSignature(signature);
        param.setVersion(version);
        param.setCACRLVersion(CACRLVersion);
        param.setSignerCRLVersion(signerCRLVersion);
        param.setSystemVersion(systemVersion);
        param.setTitleID(titleID);
        param.setTitleType(titleType);
        param.setGroupID(groupID);
        param.setAccessRights(accessRights);
        param.setTitleVersion(titleVersion);
        param.setContentCount(contentCount);
        param.setBootIndex(bootIndex);
        param.setSHA2(SHA2);
        param.setContentInfos(contentInfos);
        
        TMD result = new TMD(param);
        
        //Get Contents
        for(int i =0;i<contentCount;i++){
            buffer.position(0xB04+(0x30*i));
            byte[] content = new byte[0x30];
            buffer.get(content, 0, 0x30);
            Content c = Content.parseContent(content);
            result.setContentToIndex(c.getIndex(),c);
            result.setContentToID(c.getID(), c);
        }
        
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
