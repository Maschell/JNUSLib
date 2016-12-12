package de.mas.jnus.lib.implementations.woomy;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class WoomyMeta {
    private String name;
    private int icon;
    private List<WoomyEntry> entries;
    
    public void addEntry(String name,String folder, int entryCount){
        WoomyEntry entry = new WoomyEntry(name, folder, entryCount);       
        getEntries().add(entry);
    }
    
    public List<WoomyEntry> getEntries(){
        if(entries == null){
            setEntries(new ArrayList<>());
        }
        return entries;
    }
    
    @Data
    public class WoomyEntry {
        
        public WoomyEntry(String name, String folder, int entryCount) {
            setName(name);
            setFolder(folder);
            setEntryCount(entryCount);
        }
        
        private String name;
        private String folder;
        private int entryCount;
    }
}
