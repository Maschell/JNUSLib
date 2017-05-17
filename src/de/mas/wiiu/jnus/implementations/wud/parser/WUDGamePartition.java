package de.mas.wiiu.jnus.implementations.wud.parser;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WUDGamePartition extends WUDPartition {
    private final byte[] rawTMD;
    private final byte[] rawCert;
    private final byte[] rawTicket;

    public WUDGamePartition(String partitionName, long partitionOffset, byte[] rawTMD, byte[] rawCert, byte[] rawTicket) {
        super(partitionName, partitionOffset);
        this.rawTMD = rawTMD;
        this.rawCert = rawCert;
        this.rawTicket = rawTicket;
    }
}
