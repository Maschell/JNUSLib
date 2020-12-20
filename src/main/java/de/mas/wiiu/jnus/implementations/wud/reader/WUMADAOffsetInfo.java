package de.mas.wiiu.jnus.implementations.wud.reader;

import lombok.Data;

@Data
public class WUMADAOffsetInfo {
    private final long offset;
    private final long size;
    private final long targetOffset;
    private final boolean emptyContent;
}
