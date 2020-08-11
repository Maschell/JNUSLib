package de.mas.wiiu.jnus.implementations.wud.content.partitions;

import de.mas.wiiu.jnus.entities.FST.FST;
import lombok.Getter;

public class WiiUDataPartition extends WiiUPartition {

    @Getter private final FST fst;

    public WiiUDataPartition(WiiUPartition partition, FST fst) {
        this.setFileSystemDescriptor(partition.getFileSystemDescriptor());
        this.setVolumeID(partition.getVolumeID());
        this.getVolumes().putAll(partition.getVolumes());
        this.fst = fst;
    }

}
