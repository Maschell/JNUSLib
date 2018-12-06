package de.mas.wiiu.jnus.implementations.wud.parser;

import java.util.ArrayList;
import java.util.List;

import de.mas.wiiu.jnus.entities.fst.FST;
import lombok.Getter;
import lombok.val;
import lombok.extern.java.Log;

@Log
public class WUDGIPartition extends WUDPartition {
    @Getter private final List<WUDGIPartitionTitle> titles = new ArrayList<>();

    public WUDGIPartition(String partitionName, long partitionOffset, FST fst) {
        super(partitionName, partitionOffset);
        for (val curDir : fst.getRoot().getDirChildren()) {
            titles.add(new WUDGIPartitionTitle(fst, curDir, partitionOffset));
        }
    }

}
