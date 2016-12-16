package de.mas.jnus.lib.entities.content;

import lombok.Data;

@Data
public class ContentFSTInfoParam {
    private long offsetSector = 0;
    private long sizeSector = 0;
    private long ownerTitleID = 0;
    private int groupID = 0;
    private byte unkown = 0;
}
