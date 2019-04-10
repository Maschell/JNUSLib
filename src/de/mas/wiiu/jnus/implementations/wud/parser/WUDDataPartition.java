package de.mas.wiiu.jnus.implementations.wud.parser;

import de.mas.wiiu.jnus.entities.fst.FST;
import lombok.Getter;

public class WUDDataPartition extends WUDPartition {
    @Getter private final FST FST;

    public WUDDataPartition(String partitionName, long partitionOffset, FST curFST) {
        super(partitionName, partitionOffset);
        this.FST = curFST;
    }

}
