package de.mas.wiiu.jnus.entities.FST.nodeentry;

import java.nio.ByteBuffer;

import de.mas.wiiu.jnus.entities.FST.sectionentry.SectionEntries;
import de.mas.wiiu.jnus.entities.FST.stringtable.StringTable;
import de.mas.wiiu.jnus.utils.ByteUtils;
import de.mas.wiiu.jnus.utils.blocksize.SectionAddress;
import de.mas.wiiu.jnus.utils.blocksize.SectionBlockSize;
import lombok.Getter;

public class FileEntry extends NodeEntry {
    protected SectionAddress address = SectionAddress.empty();
    @Getter protected long size;

    public static NodeEntry parseData(byte[] data, int offset, NodeEntryParam param, SectionEntries sectionEntries, StringTable stringTable,
            SectionBlockSize blockSize) {
        FileEntry entry = new FileEntry();

        entry.entryNumber = param.entryNumber;
        entry.parent = param.parent;
        entry.entryType = param.type;
        entry.nameString = stringTable.getStringEntry(param.uint24);

        entry.address = new SectionAddress(blockSize, ByteUtils.getUnsingedIntFromBytes(data, offset + 4) & 0xFFFFFFFF);
        entry.size = ByteUtils.getUnsingedIntFromBytes(data, offset + 8) & 0xFFFFFFFF;

        entry.permission = param.permission;
        entry.sectionEntry = sectionEntries.get(param.sectionNumber);

        return entry;
    }

    public long getOffset() {
        return address.getAddressInBytes();
    }

    @Override
    public String toString() {
        return "FileEntry [address=" + String.format("0x%08X", address.getAddressInBytes()) + ", size=" + String.format("0x%08X", size) + ", entryNumber="
                + entryNumber + ", entryType=" + entryType + ", permission=" + permission + ", nameString=" + nameString + "]";
    }

    @Override
    protected byte[] getAsBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(NodeEntry.LENGTH);
        byte type = (byte) (entryType.getAsValue() & 0xFF);
        int typeAndName = (nameString.getAddress() & 0x00FFFFFF) | (type << 24);
        buffer.putInt(0, typeAndName);

        buffer.putInt(4, (int) address.getValue());
        buffer.putInt(8, (int) size);

        buffer.putShort(12, (short) permission.getValue());
        buffer.putShort(14, (short) sectionEntry.getSectionNumber());

        return buffer.array();
    }

}
