package de.mas.wiiu.jnus.entities.FST.nodeentry;

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.mas.wiiu.jnus.entities.FST.sectionentry.SectionEntries;
import de.mas.wiiu.jnus.entities.FST.stringtable.StringTable;
import de.mas.wiiu.jnus.utils.ByteUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.val;

public class DirectoryEntry extends NodeEntry {
    @Getter @Setter protected int parentEntryNumber;

    @Getter @Setter protected int lastEntryNumber;

    @Getter private final List<NodeEntry> children = new ArrayList<>();

    static DirectoryEntry parseData(byte[] data, int offset, NodeEntryParam param, SectionEntries sectionEntries, StringTable stringTable) {

        DirectoryEntry directoryEntry = new DirectoryEntry();
        directoryEntry.entryNumber = param.entryNumber;
        directoryEntry.parent = param.parent;
        directoryEntry.entryType = param.type;
        directoryEntry.nameString = stringTable.getStringEntry(param.uint24);

        directoryEntry.parentEntryNumber = (int) (ByteUtils.getUnsingedIntFromBytes(data, offset + 4) & 0xFFFFFFFF);
        directoryEntry.lastEntryNumber = (int) (ByteUtils.getUnsingedIntFromBytes(data, offset + 8) & 0xFFFFFFFF);

        directoryEntry.permission = param.permission;

        if (param.sectionNumber > sectionEntries.size()) {
            throw new IllegalArgumentException("FST_M_Error_Broken_NodeEntry");
        }
        directoryEntry.sectionEntry = sectionEntries.get(param.sectionNumber);

        return directoryEntry;
    }

    @Override
    public void printRecursive(PrintStream out, int space) {
        super.printRecursive(out, space);
        for (val child : children) {
            child.printRecursive(out, space + 5);
        }
    }

    @Override
    public void printFullPathRecursive(PrintStream out) {
        super.printFullPathRecursive(out);
        for (val child : children) {
            child.printFullPathRecursive(out);
        }
    }

    @Override
    public void printToStringRecursive(PrintStream out) {
        super.printToStringRecursive(out);
        for (val child : children) {
            child.printToStringRecursive(out);
        }
    }

    public void addChild(NodeEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("entry was empty");
        }
        children.add(entry);
    }

    public List<FileEntry> getFileChildren() {
        return getChildren().stream().filter(e -> e.isFile()).map(e -> (FileEntry) e).collect(Collectors.toList());
    }

    public List<DirectoryEntry> getDirChildren() {
        return getChildren().stream().filter(e -> e.isDirectory()).map(e -> (DirectoryEntry) e).collect(Collectors.toList());
    }

    @Override
    public Stream<NodeEntry> stream() {
        return Stream.concat(Stream.of(this), getChildren().stream().flatMap(e -> e.stream()));
    }

    @Override
    public String toString() {
        return "DirectoryEntry [entryNumber=" + entryNumber + ", entryType=" + entryType + ", permission=" + permission + ", nameString=" + nameString + "]";
    }

    @Override
    protected byte[] getAsBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(NodeEntry.LENGTH);
        byte type = (byte) (entryType.getAsValue() & 0xFF);
        int typeAndName = (nameString.getAddress() & 0x00FFFFFF) | (type << 24);
        buffer.putInt(0, typeAndName);

        buffer.putInt(4, parentEntryNumber);
        buffer.putInt(8, lastEntryNumber);

        buffer.putShort(12, (short) permission.getValue());
        buffer.putShort(14, (short) sectionEntry.getSectionNumber());

        return buffer.array();
    }
}
