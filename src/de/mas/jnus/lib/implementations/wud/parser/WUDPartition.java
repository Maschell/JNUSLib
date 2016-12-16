package de.mas.jnus.lib.implementations.wud.parser;

import lombok.Data;
@Data
public class WUDPartition {
    private final String partitionName;
    private final long partitionOffset;
            
    private WUDPartitionHeader partitionHeader;
}
