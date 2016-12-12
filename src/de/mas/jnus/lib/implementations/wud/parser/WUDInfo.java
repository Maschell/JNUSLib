package de.mas.jnus.lib.implementations.wud.parser;

import java.util.Map;
import java.util.Map.Entry;

import de.mas.jnus.lib.implementations.wud.reader.WUDDiscReader;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class WUDInfo {    
    private byte[] titleKey = null;
    
    private WUDDiscReader WUDDiscReader = null;    
    private Map<String,WUDPartition> partitions = null;
    
    @Getter(AccessLevel.PRIVATE) @Setter(AccessLevel.PROTECTED)
    private String gamePartitionName;
    
    WUDInfo(){        
    }
    
    
    private WUDGamePartition cachedGamePartition = null;
    public WUDGamePartition getGamePartition(){
        if(cachedGamePartition == null){
            cachedGamePartition = findGamePartition();
        }
        return cachedGamePartition;
    }
    private WUDGamePartition findGamePartition() {
        for(Entry<String,WUDPartition> e: getPartitions().entrySet()){
            if(e.getKey().equals(getGamePartitionName())){
                return (WUDGamePartition) e.getValue();
            }
        }
        return null;
    }
}
