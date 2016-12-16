package de.mas.jnus.lib.entities;

import de.mas.jnus.lib.entities.content.ContentInfo;
import lombok.Data;

@Data
public class TMDParam {
    private int             signatureType;                                  // 0x000
    private byte[]          signature           =   new byte[0x100];        // 0x004
    private byte[]          issuer              =   new byte[0x40];         // 0x140
    private byte            version;                                        // 0x180
    private byte            CACRLVersion;                                   // 0x181
    private byte            signerCRLVersion;                               // 0x182
    private long            systemVersion;                                  // 0x184
    private long            titleID;                                        // 0x18C    
    private int             titleType;                                      // 0x194    
    private short           groupID;                                        // 0x198 
    private byte[]          reserved            =   new byte[62];           // 0x19A    
    private int             accessRights;                                   // 0x1D8    
    private short           titleVersion;                                   // 0x1DC 
    private short           contentCount;                                   // 0x1DE 
    private short           bootIndex;                                      // 0x1E0    
    private byte[]          SHA2                =   new byte[0x20];         // 0x1E4
    private ContentInfo[]   contentInfos        =   new ContentInfo[0x40];  //
}
