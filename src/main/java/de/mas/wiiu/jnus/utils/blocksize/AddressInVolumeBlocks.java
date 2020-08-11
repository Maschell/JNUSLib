package de.mas.wiiu.jnus.utils.blocksize;

public class AddressInVolumeBlocks extends AddressInBlocks<VolumeBlockSize> {
    public AddressInVolumeBlocks(VolumeBlockSize blockSize, long value) {
        super(blockSize, value);
    }

    public static AddressInVolumeBlocks empty() {
        return new AddressInVolumeBlocks(new VolumeBlockSize(0), (long) 0);
    }
}