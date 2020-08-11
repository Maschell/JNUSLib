package de.mas.wiiu.jnus.entities.FST;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import de.mas.wiiu.jnus.entities.FST.header.Header;
import de.mas.wiiu.jnus.entities.FST.nodeentry.DirectoryEntry;
import de.mas.wiiu.jnus.entities.FST.nodeentry.EntryType;
import de.mas.wiiu.jnus.entities.FST.nodeentry.EntryType.EEntryType;
import de.mas.wiiu.jnus.entities.FST.nodeentry.FileEntry;
import de.mas.wiiu.jnus.entities.FST.nodeentry.NodeEntries;
import de.mas.wiiu.jnus.entities.FST.nodeentry.NodeEntry;
import de.mas.wiiu.jnus.entities.FST.nodeentry.Permission;
import de.mas.wiiu.jnus.entities.FST.nodeentry.RootEntry;
import de.mas.wiiu.jnus.entities.FST.sectionentry.SectionEntries;
import de.mas.wiiu.jnus.entities.FST.sectionentry.SectionEntry;
import de.mas.wiiu.jnus.entities.FST.stringtable.StringTable;
import de.mas.wiiu.jnus.utils.Utils;
import de.mas.wiiu.jnus.utils.blocksize.AddressInVolumeBlocks;
import de.mas.wiiu.jnus.utils.blocksize.SectionBlockSize;
import de.mas.wiiu.jnus.utils.blocksize.SizeInVolumeBlocks;
import de.mas.wiiu.jnus.utils.blocksize.VolumeBlockSize;
import lombok.Data;
import lombok.val;

@Data
public class FST {
    private Header header;
    private SectionEntries sectionEntries;
    private StringTable stringTable;
    private NodeEntries nodeEntries;

    public static FST parseData(byte[] data) throws IOException {
        return parseData(data, 0, new VolumeBlockSize(1));
    }

    public static FST parseData(byte[] data, int offset, VolumeBlockSize blockSize) throws IOException {
        FST fst = new FST();

        int curOffset = offset;

        fst.header = Header.parseData(Arrays.copyOfRange(data, curOffset, Header.LENGTH), 0);
        curOffset += Header.LENGTH;
        fst.sectionEntries = SectionEntries.parseData(data, curOffset, fst.header.getNumberOfSections(), blockSize);
        curOffset += fst.sectionEntries.getSizeInBytes();
        int lastEntryNumber = RootEntry.parseLastEntryNumber(data, curOffset);

        fst.stringTable = StringTable.parseData(data, curOffset + (lastEntryNumber * 16), lastEntryNumber);
        fst.nodeEntries = NodeEntries.parseData(data, curOffset, fst.sectionEntries, fst.stringTable, fst.header.getBlockSize());

        return fst;
    }
    
    public RootEntry getRootEntry() {
        return getNodeEntries().getRootEntry();
    }

    public byte[] getAsBytes() {
        byte[] headerData = header.getAsBytes();
        byte[] sectionData = sectionEntries.getAsBytes();
        byte[] stringTableData = stringTable.getAsBytes();
        byte[] nodeEntriesData = nodeEntries.getAsBytes();

        ByteBuffer buffer = ByteBuffer.allocate(headerData.length + sectionData.length + stringTableData.length + nodeEntriesData.length);
        buffer.put(headerData);
        buffer.put(sectionData);
        buffer.put(nodeEntriesData);
        buffer.put(stringTableData);
        return buffer.array();
    }

    public static FST createFromFolder(File folder) {
        Header header = new Header();
        header.setFSTVersion((short) 0);
        header.setHashDisabled((short) 1);
        header.setBlockSize(new SectionBlockSize(32));

        VolumeBlockSize blockSize = new VolumeBlockSize(32768);

        StringTable stringtable = new StringTable(getStringsFromFolder(folder));

        SectionEntry fstSection = new SectionEntry(0, "FST");
        fstSection.setAddress(new AddressInVolumeBlocks(blockSize, 0L));
        // for some reason this is always 0. We still need to adjust the offset of the next sections properly though.
        fstSection.setSize(new SizeInVolumeBlocks(blockSize, 0L));
        fstSection.setHashMode((short) 0);

        SectionEntry codeSection = new SectionEntry(1, "CODE");
        // Add real FST Size.
        codeSection.setAddress(new AddressInVolumeBlocks(blockSize, 0L));
        codeSection.setHashMode((short) 1);

        SectionEntry filesSection = new SectionEntry(2, "FILES");
        // Add real FST Size.
        filesSection.setAddress(new AddressInVolumeBlocks(blockSize, 0L));
        filesSection.setHashMode((short) 2);

        SectionEntries sections = new SectionEntries();

        sections.add(fstSection);
        sections.add(codeSection);
        sections.add(filesSection);

        DirectoryEntry dir = new DirectoryEntry();
        dir.setEntryType(new EntryType(EEntryType.Directory));
        dir.setParent(null);
        dir.setEntryNumber(0);
        dir.setNameString(stringtable.getEntry("").get());
        dir.setPermission(new Permission(0));
        dir.setSectionEntry(fstSection);

        RootEntry root = new RootEntry(dir);

        // Split files per folder. the /code folder seems to have an own section?
        File[] codeFolder = folder.listFiles(f -> f.isDirectory() && "code".contentEquals(f.getName()));
        if (codeFolder.length > 0) {
            NodeEntry res = NodeEntries.createFromFolder(codeFolder[0], header.getBlockSize(), stringtable, codeSection, new Permission(0));
            root.addChild(res);
            // res.printToStringRecursive();
            codeSection.setSize(calculateSectionSizeByFSTEntries((int) blockSize.getBlockSize(), res));
        }

        File[] otherFolder = folder.listFiles(f -> f.isDirectory() && !"code".contentEquals(f.getName()));
        for (val curFolder : otherFolder) {
            NodeEntry res = NodeEntries.createFromFolder(curFolder, header.getBlockSize(), stringtable, filesSection, new Permission(0));
            root.addChild(res);
            // res.printToStringRecursive();
            filesSection.setSize(calculateSectionSizeByFSTEntries((int) blockSize.getBlockSize(), res));
        }

        setEntryNumbers(root, 0);

        // root.printToStringRecursive();

        NodeEntries nodeEntries = new NodeEntries(root);

        header.setNumberOfSections(sections.size());

        FST fst = new FST();
        fst.setHeader(header);
        fst.setSectionEntries(sections);
        fst.setStringTable(stringtable);
        fst.setNodeEntries(nodeEntries);

        // Fix FST section length
        // int length = fst.getAsBytes().length;
        // fstSection.setSize(new VolumeLbaSize(blockSize, (length / blockSize.getBlockSize())+1));

        long sectionAddressInBlocks = 2;
        for (SectionEntry s : sections) {
            if (s.getSectionNumber() == 0) {
                continue;
            }
            s.setAddress(new AddressInVolumeBlocks(blockSize, sectionAddressInBlocks));
            sectionAddressInBlocks += s.getSize().getValue();
        }

        return fst;
    }

    private static SizeInVolumeBlocks calculateSectionSizeByFSTEntries(int blockSize, NodeEntry input) {
        // calculate size of section.
        Optional<FileEntry> lastEntry = input.stream().filter(e -> e instanceof FileEntry).map(f -> (FileEntry) f)
                .max((a, b) -> Long.compare(a.getOffset(), b.getOffset()));
        if (!lastEntry.isPresent()) {
            throw new IllegalArgumentException("WTF?");
        }
        long sectionSize = lastEntry.get().getOffset() + lastEntry.get().getSize();
        sectionSize = Utils.align(sectionSize, blockSize);
        int sectionSizeInBlocks = (int) (sectionSize / blockSize);
        sectionSizeInBlocks = sectionSize % blockSize == 0 ? sectionSizeInBlocks : sectionSizeInBlocks + 1;
        return new SizeInVolumeBlocks(new VolumeBlockSize(blockSize), (long) sectionSizeInBlocks);
    }

    public static int setEntryNumbers(NodeEntry entry, int curEntryNumber) {
        entry.setEntryNumber(curEntryNumber);
        curEntryNumber++;
        if (entry instanceof DirectoryEntry) {
            for (NodeEntry child : ((DirectoryEntry) entry).getChildren()) {
                curEntryNumber = setEntryNumbers(child, curEntryNumber);
            }
            ((DirectoryEntry) entry).setLastEntryNumber(curEntryNumber);
            ((DirectoryEntry) entry).setParentEntryNumber(entry.getEntryNumber());
        }
        return curEntryNumber;

    }

    public static List<String> getStringsFromFolder(File input) {
        List<String> result = new LinkedList<>();

        for (File f : input.listFiles()) {
            result.add(f.getName());
            if (f.isDirectory()) {
                result.addAll(getStringsFromFolder(f));
            }
        }

        return result;
    }
}
