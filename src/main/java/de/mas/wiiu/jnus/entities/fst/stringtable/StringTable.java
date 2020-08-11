package de.mas.wiiu.jnus.entities.FST.stringtable;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

public class StringTable {
    private final Map<Integer, StringEntry> stringEntries = new HashMap<>();
    private final Map<Integer, String> strings = new HashMap<>();

    public StringTable(String... strings) {
        this(Arrays.asList(strings));
    }

    public StringTable(Collection<String> strings) {
        int curLength = 0;
        this.strings.put(curLength, "");
        this.stringEntries.put(curLength, new StringEntry(this, curLength));
        curLength = 1;
        for (String s : strings) {
            this.stringEntries.put(curLength, new StringEntry(this, curLength));
            this.strings.put(curLength, s);
            curLength += s.length() + 1;
        }
    }

    private StringTable() {

    }

    public static StringTable parseData(byte[] data, long offset, long stringsCount) {
        StringTable stringTable = new StringTable();
        long curOffset = offset;
        int i;
        for (i = 0; curOffset < data.length && i < stringsCount; ++curOffset) {
            if (data[(int) curOffset] == (byte) 0) {
                ++i;
            }
        }
        if (i < stringsCount) {
            throw new IllegalArgumentException("StringTable is broken");
        }

        String[] strArray = new String(Arrays.copyOfRange(data, (int) offset, (int) (curOffset))).split("\0");
        int curLength = 0;
        for (i = 0; i < strArray.length; i++) {
            stringTable.stringEntries.put(curLength, new StringEntry(stringTable, curLength));
            stringTable.strings.put(curLength, strArray[i]);
            curLength += strArray[i].length() + 1;
        }

        return stringTable;
    }

    public String getByAddress(int address) {
        return strings.get(address);
    }

    public StringEntry getStringEntry(int address) {
        StringEntry entry = stringEntries.get(address);
        if (entry == null) {
            throw new IllegalArgumentException("Failed to find string entry for address: " + address);
        }
        return entry;
    }

    public long getSize() {
        int capacity = 1; // root entry
        for (String s : this.strings.values()) {
            capacity += s.length() + 1;
        }
        return capacity;
    }

    public Optional<StringEntry> getEntry(String str) {
        return stringEntries.entrySet().stream().filter(e -> e.getValue().toString().equals(str)).map(e -> e.getValue()).findFirst();
    }

    public byte[] getAsBytes() {
        ByteBuffer buffer = ByteBuffer.allocate((int) getSize());
        for (Entry<Integer, String> s : this.strings.entrySet()) {
            buffer.position(s.getKey());
            buffer.put(s.getValue().getBytes());
        }
        return buffer.array();
    }

}
