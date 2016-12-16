package de.mas.jnus.lib.entities.fst;

import de.mas.jnus.lib.entities.content.Content;
import lombok.Data;
@Data
public class FSTEntryParam {
    private String filename = "";
    private String path = "";

    private FSTEntry parent = null;
    
    private short flags;
      
    private long fileSize = 0;  
    private long fileOffset = 0;    
    
    private Content content = null;

    private boolean isDir = false;
    private boolean isRoot = false;
    private boolean notInPackage = false;
    
    private short contentFSTID = 0; 
}
