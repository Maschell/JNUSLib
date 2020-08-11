package de.mas.wiiu.jnus.utils.blocksize;

import lombok.Getter;

public abstract class BlockSize {
    @Getter protected long blockSize;

    public BlockSize(long blockSize) {
        this.blockSize = blockSize;
    }

    public BlockSize(BlockSize copy) {
        this.blockSize = copy.blockSize;
    }

    @Override
    public String toString() {
        return Long.toString(blockSize);
    }
}
