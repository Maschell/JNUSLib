package de.mas.wiiu.jnus.utils.blocksize;

public class SectionAddress extends AddressInBlocks<SectionBlockSize> {
    public SectionAddress(SectionBlockSize blockSize, long value) {
        super(blockSize, value);
    }

    public static SectionAddress empty() {
        return new SectionAddress(new SectionBlockSize(0), (long) 0);
    }
}
