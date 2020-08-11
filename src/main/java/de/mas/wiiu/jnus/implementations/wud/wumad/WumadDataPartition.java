package de.mas.wiiu.jnus.implementations.wud.wumad;

import de.mas.wiiu.jnus.entities.FST.FST;
import lombok.Getter;

public class WumadDataPartition extends WumadPartition {

    @Getter private final FST FST;

    public WumadDataPartition(String partitionName, FST fst) {
        super(partitionName);
        this.FST = fst;
    }

}
