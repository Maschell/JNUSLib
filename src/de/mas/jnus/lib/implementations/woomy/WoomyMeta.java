package de.mas.jnus.lib.implementations.woomy;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class WoomyMeta {
    private final String name;
    private final int icon;
    private final List<WoomyEntry> entries = new ArrayList<>();
    
    public void addEntry(String name,String folder, int entryCount){
        WoomyEntry entry = new WoomyEntry(name, folder, entryCount);       
        getEntries().add(entry);
    }
    
    @Data
    public class WoomyEntry {
        private final String name;
        private final String folder;
        private final int entryCount;
    }
}
