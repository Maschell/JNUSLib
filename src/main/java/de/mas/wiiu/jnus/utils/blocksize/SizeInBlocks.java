package de.mas.wiiu.jnus.utils.blocksize;

import lombok.Data;

@Data
public abstract class SizeInBlocks<T extends BlockSize> {
    private final T blockSize;
    private final long value;

    public long getSizeInBytes() {
        return this.getValue() * this.getBlockSize().blockSize;
    }
}
