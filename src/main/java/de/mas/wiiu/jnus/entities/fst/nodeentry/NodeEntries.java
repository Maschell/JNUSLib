package de.mas.wiiu.jnus.entities.FST.nodeentry;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.mas.wiiu.jnus.entities.FST.nodeentry.EntryType.EEntryType;
import de.mas.wiiu.jnus.entities.FST.sectionentry.SectionEntries;
import de.mas.wiiu.jnus.entities.FST.sectionentry.SectionEntry;
import de.mas.wiiu.jnus.entities.FST.stringtable.StringEntry;
import de.mas.wiiu.jnus.entities.FST.stringtable.StringTable;
import de.mas.wiiu.jnus.utils.Utils;
import de.mas.wiiu.jnus.utils.blocksize.SectionAddress;
import de.mas.wiiu.jnus.utils.blocksize.SectionBlockSize;
import lombok.Getter;
import lombok.val;

public class NodeEntries {

    @Getter private final RootEntry rootEntry;

    public NodeEntries(RootEntry rootEntry) {
        this.rootEntry = rootEntry;
    }

    private static NodeEntry DeserializeImpl(byte[] data, int offset, DirectoryEntry parent, int entryNumber, SectionEntries sectionEntries,
            StringTable stringTable, SectionBlockSize blockSize) {
        NodeEntry nodeEntry = NodeEntry.AutoDeserialize(data, offset, parent, entryNumber, sectionEntries, stringTable, blockSize);
        if (nodeEntry instanceof DirectoryEntry) {
            DirectoryEntry dirNode = (DirectoryEntry) nodeEntry;
            long curEntryNumber = dirNode.getEntryNumber() + 1;
            while (curEntryNumber < dirNode.getLastEntryNumber()) {
                NodeEntry entry = NodeEntries.DeserializeImpl(data, offset + ((int) curEntryNumber - (int) dirNode.getEntryNumber()) * NodeEntry.LENGTH,
                        dirNode, (int) curEntryNumber, sectionEntries, stringTable, blockSize);
                dirNode.addChild(entry);
                if (entry instanceof DirectoryEntry) {
                    curEntryNumber = ((DirectoryEntry) entry).getLastEntryNumber();
                } else {
                    curEntryNumber++;
                }
            }
        }
        return nodeEntry;
    }

    public static NodeEntries parseData(byte[] data, int offset, SectionEntries sectionEntries, StringTable stringTable, SectionBlockSize blockSize) {
        NodeEntry rootEntry = NodeEntries.DeserializeImpl(data, offset, (DirectoryEntry) null, 0, sectionEntries, stringTable, blockSize);
        if (rootEntry instanceof RootEntry) {
            return new NodeEntries((RootEntry) rootEntry);
        }
        throw new IllegalArgumentException("FST_NOT_ROOT_ENTRY");

    }

    public static NodeEntry createFromFolder(File codeFolder, SectionBlockSize blockSize, StringTable stringtable, SectionEntry section,
            Permission permission) {

        NodeEntry cur = createFromFile(codeFolder, null, stringtable, section, permission);

        val tmpList = cur.stream().filter(f -> f.isFile()).collect(Collectors.toList());

        // calcuate offsets
        SectionAddress curAddress = new SectionAddress(blockSize, 0L);
        for (val e : tmpList) {
            ((FileEntry) e).address = new SectionAddress(curAddress.getBlockSize(), curAddress.getValue());
            val size = ((FileEntry) e).getSize();
            int alignedSize = (int) Utils.align(size, (int) blockSize.getBlockSize());
            int fstBlockSize = (int) blockSize.getBlockSize();
            int offsetDiffInBlocks = alignedSize / fstBlockSize;
            long newOffset = alignedSize % fstBlockSize == 0 ? offsetDiffInBlocks : offsetDiffInBlocks + 1;
            curAddress = new SectionAddress(blockSize, newOffset + curAddress.getValue());
        }

        return cur;
    }

    private static NodeEntry createFromFile(File inputFile, DirectoryEntry parent, StringTable stringtable, SectionEntry section, Permission permission) {

        StringEntry name = stringtable.getEntry(inputFile.getName()).orElseThrow(() -> new IllegalArgumentException("Failed to find String"));

        NodeEntry result = null;
        if (inputFile.isDirectory()) {
            DirectoryEntry curDir = new DirectoryEntry();
            for (File curFile : inputFile.listFiles()) {
                NodeEntry child = createFromFile(curFile, curDir, stringtable, section, permission);
                curDir.addChild(child);
            }
            curDir.setEntryType(new EntryType(EEntryType.Directory));
            result = curDir;
        } else {
            result = new RealFileEntry(inputFile);
            result.setEntryType(new EntryType(EEntryType.File));
        }

        result.setParent(parent);
        result.setPermission(permission);
        result.setNameString(name);
        result.setSectionEntry(section);
        return result;
    }

    public long getSizeInBytes() {
        return NodeEntry.LENGTH * rootEntry.getLastEntryNumber();
    }

    public byte[] getAsBytes() {
        ByteBuffer buffer = ByteBuffer.allocate((int) getSizeInBytes());
        for (NodeEntry e : rootEntry.stream().collect(Collectors.toList())) {
            buffer.put(e.getAsBytes());
        }
        return buffer.array();
    }

    public Stream<NodeEntry> streamBy(SectionEntry e) {
        return rootEntry.stream().filter(f -> f.getSectionEntry().equals(e));
    }

    public List<NodeEntry> getBy(SectionEntry e) {
        return rootEntry.stream().filter(f -> f.getSectionEntry().equals(e)).collect(Collectors.toList());
    }
}
