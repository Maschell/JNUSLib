package de.mas.wiiu.jnus.entities.FST.nodeentry;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import lombok.Getter;

public class EntryType {
    private final Set<EntryType.EEntryType> entryTypes = new TreeSet<>();

    public EntryType(EEntryType type) {
        this.entryTypes.add(type);
    }

    public EntryType(EEntryType... types) {
        Collections.addAll(entryTypes, types);
    }

    @Override
    public String toString() {
        return "EntryType [entryTypes=" + entryTypes + "]";
    }

    public EntryType(int type) {
        for (EEntryType v : EEntryType.values()) {
            if ((v.getValue() & type) == v.getValue()) {
                entryTypes.add(v);
            }
        }

        if (entryTypes.contains(EEntryType.Directory)) {
            entryTypes.remove(EEntryType.File);
        }

    }

    public boolean has(EEntryType val) {
        return entryTypes.contains(val);
    }

    public enum EEntryType {
        File(0), Directory(1), Link(0x80);

        @Getter private int value;

        EEntryType(int val) {
            this.value = val;
        }

        public static EEntryType findByValue(int val) {
            for (EEntryType v : values()) {
                if (v.getValue() == val) {
                    return v;
                }
            }
            return null;
        }

    }

    public int getAsValue() {
        int val = 0;

        for (EEntryType e : entryTypes) {
            val |= e.getValue();
        }
        return val;
    }

}
