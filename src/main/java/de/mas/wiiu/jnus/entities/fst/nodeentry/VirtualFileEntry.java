package de.mas.wiiu.jnus.entities.FST.nodeentry;

import de.mas.wiiu.jnus.entities.FST.nodeentry.EntryType.EEntryType;
import de.mas.wiiu.jnus.entities.FST.sectionentry.SectionEntry;
import de.mas.wiiu.jnus.entities.FST.stringtable.StringEntry;

public class VirtualFileEntry extends FileEntry {
    private long offset;

    public static FileEntry create(DirectoryEntry parent, long offset, long size, StringEntry name, Permission permission, SectionEntry section) {
        VirtualFileEntry entry = new VirtualFileEntry();

        entry.entryNumber = 0;
        entry.parent = parent;
        entry.entryType = new EntryType(EEntryType.File);
        entry.nameString = name;

        entry.offset = offset;
        entry.size = size;
        entry.permission = permission;
        entry.sectionEntry = section;

        return entry;
    }

    @Override
    public long getOffset() {
        return offset;
    }
}
