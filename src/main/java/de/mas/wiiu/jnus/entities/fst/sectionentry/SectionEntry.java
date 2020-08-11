package de.mas.wiiu.jnus.entities.FST.sectionentry;

import java.nio.ByteBuffer;

import de.mas.wiiu.jnus.utils.ByteUtils;
import de.mas.wiiu.jnus.utils.blocksize.AddressInVolumeBlocks;
import de.mas.wiiu.jnus.utils.blocksize.SizeInVolumeBlocks;
import de.mas.wiiu.jnus.utils.blocksize.VolumeBlockSize;
import lombok.Data;

@Data
public class SectionEntry {
    public static final int LENGTH = 32;
    private final int sectionNumber;
    private final String name;
    private AddressInVolumeBlocks address;
    private SizeInVolumeBlocks size = new SizeInVolumeBlocks(new VolumeBlockSize(0), (long) 0);
    private long ownerID;
    private long groupID;
    private short hashMode;

    public static SectionEntry parseData(byte[] data, long offset, String sectionName, int sectionNumber, VolumeBlockSize blockSize) {
        SectionEntry sectionEntry = new SectionEntry(sectionNumber, sectionName);

        sectionEntry.address = new AddressInVolumeBlocks(blockSize, ByteUtils.getUnsingedIntFromBytes(data, (int) offset) & 0xFFFFFFFF);
        sectionEntry.size = new SizeInVolumeBlocks(blockSize, ByteUtils.getUnsingedIntFromBytes(data, (int) offset + 4) & 0xFFFFFFFF);
        sectionEntry.ownerID = ByteUtils.getLongFromBytes(data, (int) offset + 8);
        sectionEntry.groupID = ByteUtils.getUnsingedIntFromBytes(data, (int) offset + 16) & 0xFFFFFFFF;
        sectionEntry.hashMode = ByteUtils.getByteFromBytes(data, (int) offset + 20);

        return sectionEntry;
    }

    public byte[] getAsBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(LENGTH);
        buffer.putInt(0, (int) address.getValue());
        buffer.putInt(4, (int) size.getValue());
        buffer.putLong(8, ownerID);
        buffer.putInt(16, (int) groupID);
        buffer.put(20, (byte) hashMode);
        return buffer.array();
    }

    @Override
    public String toString() {
        return "SectionEntry [sectionNumber=" + sectionNumber + ", name=" + name + ", address=" + (address.getAddressInBytes() / 1024.0)  + " KiB , size="
                + (size.getSizeInBytes() / 1024.0) + " KiB , ownerID=" + String.format("%016X", ownerID) + ", groupID=" + String.format("%04X", groupID)
                + ", hashMode=" + hashMode + "]";
    }
}
