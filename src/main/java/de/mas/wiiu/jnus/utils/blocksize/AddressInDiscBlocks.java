package de.mas.wiiu.jnus.utils.blocksize;

public class AddressInDiscBlocks extends AddressInBlocks<DiscBlockSize> {
    public AddressInDiscBlocks(DiscBlockSize blockSize, long value) {
        super(blockSize, value);
    }

    public static AddressInDiscBlocks empty() {
        return new AddressInDiscBlocks(new DiscBlockSize(0), (long) 0);
    }
}