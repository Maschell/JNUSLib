package de.mas.wiiu.jnus.implementations.wud.content.partitions;

import lombok.Getter;

public class WiiUGMPartition extends WiiUPartition {
    @Getter private final byte[] rawTicket;
    @Getter private final byte[] rawTMD;
    @Getter private final byte[] rawCert;

    public WiiUGMPartition(WiiUPartition partition, byte[] rawTIK, byte[] rawTMD, byte[] rawCert) {
        this.setFileSystemDescriptor(partition.getFileSystemDescriptor());
        this.setVolumeID(partition.getVolumeID());
        this.getVolumes().putAll(partition.getVolumes());
        this.rawCert = rawCert;
        this.rawTMD = rawTMD;
        this.rawTicket = rawTIK;
    }
}
