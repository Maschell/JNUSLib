package de.mas.jnus.lib.entities.content;

import lombok.Data;
@Data
public class ContentParam {
    private int ID = 0x00;
    private short index = 0x00;     
    private short type = 0x0000;
        
    private long encryptedFileSize = 0;
    private byte[] SHA2Hash = new byte[0x14];
        
    private ContentFSTInfo contentFSTInfo = null;
}
