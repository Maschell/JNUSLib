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
package de.mas.wiiu.jnus.implementations.wud.parser;

import java.text.ParseException;

import de.mas.wiiu.jnus.entities.TMD;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WUDGamePartition extends WUDPartition {
    private final TMD tmd;
    private final byte[] rawTMD;
    private final byte[] rawCert;
    private final byte[] rawTicket;

    public WUDGamePartition(String partitionName, long partitionOffset, byte[] rawTMD, byte[] rawCert, byte[] rawTicket) throws ParseException {
        super(partitionName, partitionOffset);
        this.rawTMD = rawTMD;
        this.tmd = TMD.parseTMD(rawTMD);
        this.rawCert = rawCert;
        this.rawTicket = rawTicket;
    }
}