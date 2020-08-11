package de.mas.wiiu.jnus.entities.FST.nodeentry;

import de.mas.wiiu.jnus.entities.FST.nodeentry.EntryType.EEntryType;
import de.mas.wiiu.jnus.entities.FST.sectionentry.SectionEntries;
import de.mas.wiiu.jnus.entities.FST.stringtable.StringTable;
import de.mas.wiiu.jnus.utils.ByteUtils;

public class RootEntry extends DirectoryEntry {
    public RootEntry(DirectoryEntry input) {
        if (!input.entryType.has(EEntryType.Directory) || input.entryNumber != 0) {
            throw new IllegalArgumentException("Input is no root entry.");
        }

        this.entryNumber = input.entryNumber;
        this.parent = input.parent;
        this.nameString = input.nameString;
        this.entryType = input.entryType;

        this.parentEntryNumber = input.parentEntryNumber;
        this.lastEntryNumber = input.lastEntryNumber;
        this.permission = input.permission;
        this.sectionEntry = input.sectionEntry;
    }

    public static int parseLastEntryNumber(byte[] data, long offset) {
        return (int) (ByteUtils.getUnsingedIntFromBytes(data, (int) (offset + 8)) & 0xFFFFFFFF);
    }

    public static RootEntry parseData(byte[] data, int offset, NodeEntryParam param, SectionEntries sectionEntries, StringTable stringTable) {
        return new RootEntry(DirectoryEntry.parseData(data, offset, param, sectionEntries, stringTable));
    }
}
