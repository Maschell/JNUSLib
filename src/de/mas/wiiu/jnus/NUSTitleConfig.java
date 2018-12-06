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
package de.mas.wiiu.jnus;

import de.mas.wiiu.jnus.entities.Ticket;
import de.mas.wiiu.jnus.implementations.woomy.WoomyInfo;
import de.mas.wiiu.jnus.implementations.wud.parser.WUDGIPartitionTitle;
import de.mas.wiiu.jnus.implementations.wud.parser.WUDGamePartition;
import de.mas.wiiu.jnus.implementations.wud.parser.WUDInfo;
import de.mas.wiiu.jnus.implementations.wud.reader.WUDDiscReader;
import lombok.Data;

@Data
public class NUSTitleConfig {
    private String inputPath;
    private WUDGamePartition WUDGamePartition = null;
    private WUDGIPartitionTitle WUDGIPartitionTitle = null;
    private WUDInfo WUDInfo;
    private Ticket ticket;

    private int version = Settings.LATEST_TMD_VERSION;
    private long titleID = 0x0L;

    private WoomyInfo woomyInfo;
}
