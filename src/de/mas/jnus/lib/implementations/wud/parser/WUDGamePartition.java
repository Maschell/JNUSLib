package de.mas.jnus.lib.implementations.wud.parser;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=true)
public class WUDGamePartition extends WUDPartition {
    private byte[] rawTMD;
    private byte[] rawCert;
    private byte[] rawTicket;
}
