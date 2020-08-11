package de.mas.wiiu.jnus.entities.FST.sectionentry;

import java.nio.ByteBuffer;
import java.util.LinkedList;

import de.mas.wiiu.jnus.utils.blocksize.VolumeBlockSize;

public class SectionEntries extends LinkedList<SectionEntry> {
    private static final long serialVersionUID = 1L;

    public static SectionEntries parseData(byte[] data, long offset, long numberOfSections, VolumeBlockSize blockSize) {
        SectionEntries sectionEntries = new SectionEntries();
        for (int i = 0; i < numberOfSections; i++) {
            sectionEntries.add(SectionEntry.parseData(data, offset + (i * 32), "Section: " + i, i, blockSize));
        }

        return sectionEntries;
    }

    public long getSizeInBytes() {
        return size() * SectionEntry.LENGTH;
    }

    public byte[] getAsBytes() {
        ByteBuffer buffer = ByteBuffer.allocate((int) getSizeInBytes());
        for (SectionEntry entry : this) {
            buffer.put(entry.getAsBytes());
        }
        return buffer.array();
    }
}
