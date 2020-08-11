package de.mas.wiiu.jnus.entities.FST.stringtable;

import lombok.Data;

@Data
public class StringEntry {
    private final StringTable stringTable;
    private final int address;

    @Override
    public String toString() {
        return stringTable.getByAddress(address);
    }

}
