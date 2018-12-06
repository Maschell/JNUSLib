/****************************************************************************
 * Copyright (C) 2016-2018 Maschell
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ****************************************************************************/
package de.mas.wiiu.jnus.implementations.wud.parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public void addPartion(String partitionName, WUDGamePartition partition) {
        getPartitions().put(partitionName, partition);
    }

    public List<WUDGamePartition> getGamePartitions() {
        return partitions.values().stream().filter(p -> p instanceof WUDGamePartition).map(p -> (WUDGamePartition) p).collect(Collectors.toList());
    }

    public List<WUDGIPartitionTitle> getGIPartitionTitles() {
        return partitions.values().stream().filter(p -> p instanceof WUDGIPartition).flatMap(p -> ((WUDGIPartition) p).getTitles().stream())
                .collect(Collectors.toList());
    }
}
