package de.mas.wiiu.jnus.implementations.wud.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.mas.wiiu.jnus.implementations.wud.reader.WUDDiscReader;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class WUDInfo {
    private final byte[] titleKey;

    private final WUDDiscReader WUDDiscReader;
    private final Map<String, WUDPartition> partitions = new HashMap<>();

    @Getter(AccessLevel.PRIVATE) @Setter(AccessLevel.PROTECTED) private String gamePartitionName;

    private WUDGamePartition cachedGamePartition = null;

    public void addPartion(String partitionName, WUDGamePartition partition) {
        getPartitions().put(partitionName, partition);
    }

    public WUDGamePartition getGamePartition() {
        if (cachedGamePartition == null) {
            cachedGamePartition = findGamePartition();
        }
        return cachedGamePartition;
    }

    private WUDGamePartition findGamePartition() {
        for (Entry<String, WUDPartition> e : getPartitions().entrySet()) {
            if (e.getKey().equals(getGamePartitionName())) {
                return (WUDGamePartition) e.getValue();
            }
        }
        return null;
    }
}
