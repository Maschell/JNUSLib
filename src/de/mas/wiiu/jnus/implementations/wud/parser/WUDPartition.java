package de.mas.wiiu.jnus.implementations.wud.parser;

import lombok.Data;

@Data
public class WUDPartition {
    private final String partitionName;
    private final long partitionOffset;

    private WUDPartitionHeader partitionHeader;
}
