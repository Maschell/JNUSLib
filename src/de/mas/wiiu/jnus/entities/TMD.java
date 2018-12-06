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
package de.mas.wiiu.jnus.entities;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mas.wiiu.jnus.entities.content.Content;
import de.mas.wiiu.jnus.entities.content.ContentInfo;
import lombok.Data;
import lombok.Getter;
import lombok.extern.java.Log;

@Log
public final class TMD {
    private static final int SIGNATURE_LENGTH = 0x100;
    private static final int ISSUER_LENGTH = 0x40;
    private static final int RESERVED_LENGTH = 0x3E;
    private static final int SHA2_LENGTH = 0x20;

    private static final int POSITION_SIGNATURE = 0x04;
    private static final int POSITION_ISSUER = 0x140;
    private static final int POSITION_RESERVED = 0x19A;
    private static final int POSITION_SHA2 = 0x1E4;

    private static final int CONTENT_INFO_ARRAY_SIZE = 0x40;

    private static final int CONTENT_INFO_OFFSET = 0x204;
    private static final int CONTENT_OFFSET = 0xB04;
    
    private static final int CERT1_LENGTH     = 0x400;
    private static final int CERT2_LENGTH     = 0x300;
    
    

    @Getter private final int signatureType;                        // 0x000
    @Getter private final byte[] signature;                         // 0x004
    @Getter private final byte[] issuer;                            // 0x140
    @Getter private final byte version;                             // 0x180
    @Getter private final byte CACRLVersion;                        // 0x181
    @Getter private final byte signerCRLVersion;                    // 0x182
    @Getter private final long systemVersion;                       // 0x184
    @Getter private final long titleID;                             // 0x18C
    @Getter private final int titleType;                            // 0x194
    @Getter private final short groupID;                            // 0x198
    @Getter private final byte[] reserved;                          // 0x19A
    @Getter private final int accessRights;                         // 0x1D8
    @Getter private final short titleVersion;                       // 0x1DC
    @Getter private final short contentCount;                       // 0x1DE
    @Getter private final short bootIndex;                          // 0x1E0
    @Getter private final byte[] SHA2;                              // 0x1E4
    @Getter private final ContentInfo[] contentInfos;
    @Getter private byte[] cert1;
    @Getter private byte[] cert2;
  
    private final Map<Integer, Content> contentToIndex = new HashMap<>();
    private final Map<Integer, Content> contentToID = new HashMap<>();

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
        this.cert1 = param.getCert1();
        this.cert2 = param.getCert2();
    }

    public static TMD parseTMD(File tmd) throws IOException {
        if (tmd == null || !tmd.exists()) {
            log.info("TMD input file null or doesn't exist.");
            return null;
        }
        return parseTMD(Files.readAllBytes(tmd.toPath()));
    }

    public static TMD parseTMD(byte[] input) {
        if (input == null || input.length == 0) {
            throw new RuntimeException("Invalid TMD file.");
        }
        byte[] signature = new byte[SIGNATURE_LENGTH];
        byte[] issuer = new byte[ISSUER_LENGTH];
        byte[] reserved = new byte[RESERVED_LENGTH];
        byte[] SHA2 = new byte[SHA2_LENGTH];
        byte[] cert1 = new byte[CERT1_LENGTH];
        byte[] cert2 = new byte[CERT2_LENGTH];

        ContentInfo[] contentInfos = new ContentInfo[CONTENT_INFO_ARRAY_SIZE];

        ByteBuffer buffer = ByteBuffer.allocate(input.length);
        buffer.put(input);

        // Get Signature
        buffer.position(0);
        int signatureType = buffer.getInt();
        buffer.position(POSITION_SIGNATURE);
        buffer.get(signature, 0, SIGNATURE_LENGTH);

        // Get Issuer
        buffer.position(POSITION_ISSUER);
        buffer.get(issuer, 0, ISSUER_LENGTH);

        // Get CACRLVersion and signerCRLVersion
        buffer.position(0x180);
        byte version = buffer.get();
        byte CACRLVersion = buffer.get();
        byte signerCRLVersion = buffer.get();

        // Get title information
        buffer.position(0x184);
        long systemVersion = buffer.getLong();
        long titleID = buffer.getLong();
        int titleType = buffer.getInt();
        short groupID = buffer.getShort();

        // Get other information
        buffer.position(POSITION_RESERVED);
        buffer.get(reserved, 0, RESERVED_LENGTH);

        // Get accessRights,titleVersion,contentCount,bootIndex
        buffer.position(0x1D8);
        int accessRights = buffer.getInt();
        short titleVersion = buffer.getShort();
        short contentCount = buffer.getShort();
        short bootIndex = buffer.getShort();

        // Get hash
        buffer.position(POSITION_SHA2);
        buffer.get(SHA2, 0, SHA2_LENGTH);

        // Get contentInfos
        buffer.position(CONTENT_INFO_OFFSET);
        for (int i = 0; i < CONTENT_INFO_ARRAY_SIZE; i++) {
            byte[] contentInfo = new byte[ContentInfo.CONTENT_INFO_SIZE];
            buffer.get(contentInfo, 0, ContentInfo.CONTENT_INFO_SIZE);
            contentInfos[i] = ContentInfo.parseContentInfo(contentInfo);
        }

        List<Content> contentList = new ArrayList<>();
        // Get Contents
        for (int i = 0; i < contentCount; i++) {
            buffer.position(CONTENT_OFFSET + (Content.CONTENT_SIZE * i));
            byte[] content = new byte[Content.CONTENT_SIZE];
            buffer.get(content, 0, Content.CONTENT_SIZE);
            Content c = Content.parseContent(content);
            contentList.add(c);
        }
        
        try{
            buffer.get(cert2, 0, CERT2_LENGTH);
        }catch(Exception e){
            
        }
        
        try{
            buffer.get(cert1, 0, CERT1_LENGTH);
        }catch(Exception e){
            
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
        param.setCert1(cert1);
        param.setCert2(cert2);

        TMD result = new TMD(param);
        
        for(Content c : contentList){
            result.setContentToIndex(c.getIndex(), c);
            result.setContentToID(c.getID(), c);
        }

        return result;
    }

    public Content getContentByIndex(int index) {
        return contentToIndex.get(index);
    }

    private void setContentToIndex(int index, Content content) {
        contentToIndex.put(index, content);
    }

    public Content getContentByID(int id) {
        return contentToID.get(id);
    }

    private void setContentToID(int id, Content content) {
        contentToID.put(id, content);
    }

    /**
     * Returns all contents mapped by index
     * 
     * @return Map of Content, index/content pairs
     */
    public Map<Integer, Content> getAllContents() {
        return contentToIndex;
    }

    public void printContents() {
        long totalSize = 0;
        for (Content c : contentToIndex.values()) {
            totalSize += c.getEncryptedFileSize();
            System.out.println(c);
        }
        System.out.println("Total size: " + totalSize);

    }

    @Data
    private static class TMDParam {
        private int signatureType;                                  // 0x000
        private byte[] signature;                                   // 0x004
        private byte[] issuer;                                      // 0x140
        private byte version;                                       // 0x180
        private byte CACRLVersion;                                  // 0x181
        private byte signerCRLVersion;                              // 0x182
        private long systemVersion;                                 // 0x184
        private long titleID;                                       // 0x18C
        private int titleType;                                      // 0x194
        private short groupID;                                      // 0x198
        private byte[] reserved;                                    // 0x19A
        private int accessRights;                                   // 0x1D8
        private short titleVersion;                                 // 0x1DC
        private short contentCount;                                 // 0x1DE
        private short bootIndex;                                    // 0x1E0
        private byte[] SHA2;                                        // 0x1E4
        private ContentInfo[] contentInfos;                         //
        private byte[] cert1;
        private byte[] cert2;
    }
}
