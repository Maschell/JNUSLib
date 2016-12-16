package de.mas.jnus.lib;

import de.mas.jnus.lib.entities.Ticket;
import de.mas.jnus.lib.implementations.woomy.WoomyInfo;
import de.mas.jnus.lib.implementations.wud.parser.WUDInfo;
import lombok.Data;

@Data
public class NUSTitleConfig {
    private String inputPath;
    private WUDInfo WUDInfo;
    private Ticket ticket;
        
    private int version = Settings.LATEST_TMD_VERSION;
    private long titleID = 0x0L;
    
    private WoomyInfo woomyInfo;
}
