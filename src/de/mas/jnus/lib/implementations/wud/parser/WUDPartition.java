package de.mas.jnus.lib.implementations.wud.parser;

import lombok.Data;
@Data
public class WUDPartition {
    private String partitionName = "";
    private long partitionOffset = 0;
            
    private WUDPartitionHeader partitionHeader;
}
