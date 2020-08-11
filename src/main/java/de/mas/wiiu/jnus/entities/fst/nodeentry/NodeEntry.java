package de.mas.wiiu.jnus.entities.FST.nodeentry;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.stream.Stream;

import de.mas.wiiu.jnus.entities.FST.nodeentry.EntryType.EEntryType;
import de.mas.wiiu.jnus.entities.FST.sectionentry.SectionEntries;
import de.mas.wiiu.jnus.entities.FST.sectionentry.SectionEntry;
import de.mas.wiiu.jnus.entities.FST.stringtable.StringEntry;
import de.mas.wiiu.jnus.entities.FST.stringtable.StringTable;
import de.mas.wiiu.jnus.utils.ByteUtils;
import de.mas.wiiu.jnus.utils.blocksize.SectionBlockSize;
import lombok.Data;

@Data
public abstract class NodeEntry {
    public static final int LENGTH = 16;

    protected int entryNumber;
    protected EntryType entryType;
    protected Permission permission;
    protected StringEntry nameString;
    protected SectionEntry sectionEntry;
    protected DirectoryEntry parent;

    public static NodeEntry AutoDeserialize(byte[] data, int offset, DirectoryEntry parent, int entryNumber, SectionEntries sectionEntries,
            StringTable stringTable, SectionBlockSize blockSize) {

        byte[] curEntryData = data;
        int curOffset = (int) offset;
        if (offset > 0) {
            curEntryData = Arrays.copyOfRange(curEntryData, curOffset, curOffset + NodeEntry.LENGTH);
            curOffset = 0;
        }

        NodeEntryParam param = new NodeEntryParam();
        param.permission = new Permission((int) (ByteUtils.getShortFromBytes(data, offset + 12) & 0xFFFF));
        param.sectionNumber = (int) (ByteUtils.getShortFromBytes(data, offset + 14) & 0xFFFF);
        param.entryNumber = entryNumber;
        param.parent = parent;
        param.type = new EntryType(ByteUtils.getByteFromBytes(curEntryData, curOffset));

        byte[] entryData = Arrays.copyOfRange(curEntryData, curOffset, curOffset + 4);
        entryData[0] = 0;
        param.uint24 = ByteUtils.getIntFromBytes(entryData, 0);

        if (param.type.has(EEntryType.Directory) && param.uint24 == 0) { // Root
            return (NodeEntry) RootEntry.parseData(curEntryData, 0, param, sectionEntries, stringTable);
        } else if (param.type.has(EEntryType.Directory)) {
            return (NodeEntry) DirectoryEntry.parseData(curEntryData, 0, param, sectionEntries, stringTable);
        } else if (param.type.has(EEntryType.File)) {
            return (NodeEntry) FileEntry.parseData(curEntryData, 0, param, sectionEntries, stringTable, blockSize);
        }

        throw new IllegalArgumentException("FST_UNKNOWN_NODE_TYPE");
    }

    public String getName() {
        return nameString.toString();
    }

    protected StringBuilder getFullPathInternal() {
        if (parent != null) {
            return parent.getFullPathInternal().append('/').append(getName());
        }
        return new StringBuilder(getName());
    }

    public String getFullPath() {
        return getFullPathInternal().toString();
    }

    public String getPath() {
        if (parent != null) {
            return parent.getFullPath();
        }
        return "/";
    }

    public void printRecursive(int space) {
        printRecursive(System.out, space);
    }

    public void printRecursive(PrintStream out, int space) {
        for (int i = 0; i < space; i++) {
            out.print(" ");
        }
        out.print(getName());
        out.println();
    }

    public void printPathRecursive() {
        printFullPathRecursive(System.out);
    }

    public void printFullPathRecursive(PrintStream out) {
        out.println(getFullPath() +" " + getSectionEntry().getSectionNumber());
    }

    public void printToStringRecursive() {
        printToStringRecursive(System.out);
    }

    public void printToStringRecursive(PrintStream out) {
        out.println(toString());
    }

    public boolean isDirectory() {
        return entryType.has(EEntryType.Directory);
    }

    public boolean isFile() {
        return entryType.has(EEntryType.File);
    }

    public boolean isLink() {
        return entryType.has(EEntryType.Link);
    }

    @Override
    public String toString() {
        return "NodeEntry [entryNumber=" + entryNumber + ", entryType=" + entryType + ", permission=" + permission + ", nameString=" + nameString + "]";
    }

    @Data
    protected static class NodeEntryParam {
        public DirectoryEntry parent;
        public int entryNumber;
        public int sectionNumber;
        public Permission permission;
        protected EntryType type;
        protected int uint24;
    }

    public Stream<NodeEntry> stream() {
        return Stream.of(this);
    }

    protected abstract byte[] getAsBytes();
}
