package de.mas.wiiu.jnus.implementations.wud.wumad;

import java.text.ParseException;

import de.mas.wiiu.jnus.entities.TMD;
import de.mas.wiiu.jnus.implementations.wud.GamePartitionHeader;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WumadGamePartition extends WumadPartition {
    private final GamePartitionHeader partitionHeader;

    private final TMD tmd;
    private final byte[] rawTMD;
    private final byte[] rawCert;
    private final byte[] rawTicket;

    public WumadGamePartition(String partitionName, GamePartitionHeader partitionHeader, byte[] rawTMD, byte[] rawCert, byte[] rawTicket)
            throws ParseException {
        super(partitionName);
        this.partitionHeader = partitionHeader;
        this.rawTMD = rawTMD;
        this.tmd = TMD.parseTMD(rawTMD);
        this.rawCert = rawCert;
        this.rawTicket = rawTicket;
    }
}
