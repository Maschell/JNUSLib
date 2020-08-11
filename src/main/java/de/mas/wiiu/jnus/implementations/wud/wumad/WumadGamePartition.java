package de.mas.wiiu.jnus.implementations.wud.wumad;

import java.text.ParseException;

import de.mas.wiiu.jnus.entities.TMD.TitleMetaData;
import de.mas.wiiu.jnus.implementations.wud.content.partitions.volumes.VolumeHeader;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WumadGamePartition extends WumadPartition {
    private final VolumeHeader partitionHeader;

    private final TitleMetaData tmd;
    private final byte[] rawTMD;
    private final byte[] rawCert;
    private final byte[] rawTicket;

    public WumadGamePartition(String partitionName, VolumeHeader partitionHeader, byte[] rawTMD, byte[] rawCert, byte[] rawTicket) throws ParseException {
        super(partitionName);
        this.partitionHeader = partitionHeader;
        this.rawTMD = rawTMD;
        this.tmd = TitleMetaData.parseTMD(rawTMD);
        this.rawCert = rawCert;
        this.rawTicket = rawTicket;
    }
}
