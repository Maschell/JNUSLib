/****************************************************************************
 * Copyright (C) 2016-2019 Maschell
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
package de.mas.wiiu.jnus.implementations.wud.wumad;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import lombok.Data;
import lombok.Setter;

@Data
public class WumadInfo {

    private final List<WumadPartition> partitions = new ArrayList<>();

    public List<WumadGamePartition> getGamePartitions() {
        return partitions.stream().filter(p -> p instanceof WumadGamePartition).map(p -> (WumadGamePartition) p).collect(Collectors.toList());
    }

    @Setter private ZipFile zipFile;

    WumadInfo() {
    }
}
