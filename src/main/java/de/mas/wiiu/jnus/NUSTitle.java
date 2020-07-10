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
package de.mas.wiiu.jnus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import de.mas.wiiu.jnus.entities.TMD;
import de.mas.wiiu.jnus.entities.Ticket;
import de.mas.wiiu.jnus.entities.fst.FST;
import de.mas.wiiu.jnus.entities.fst.FSTEntry;
import de.mas.wiiu.jnus.interfaces.NUSDataProcessor;
import de.mas.wiiu.jnus.utils.FSTUtils;
import lombok.Getter;
import lombok.Setter;

public class NUSTitle {
    @Getter @Setter private Optional<FST> FST = Optional.empty();
    @Getter @Setter private Optional<Ticket> ticket;

    @Getter private final TMD TMD;

    @Getter private final NUSDataProcessor dataProcessor;

    private NUSTitle(TMD tmd, NUSDataProcessor dataProcessor) {
        this.TMD = tmd;
        this.dataProcessor = dataProcessor;
    }

    public static NUSTitle create(TMD tmd, NUSDataProcessor dataProcessor, Optional<Ticket> ticket, Optional<FST> fst) {
        NUSTitle result = new NUSTitle(tmd, dataProcessor);
        result.setTicket(ticket);
        result.setFST(fst);
        return result;
    }

    public Stream<FSTEntry> getAllFSTEntriesAsStream() {
        if (!FST.isPresent()) {
            return Stream.empty();
        }
        return FSTUtils.getAllFSTEntryChildrenAsStream(FST.get().getRoot());
    }

    public List<FSTEntry> getFSTEntriesByRegEx(String regEx) {
        return getFSTEntriesByRegEx(regEx, true);
    }

    public List<FSTEntry> getFSTEntriesByRegEx(String regEx, boolean onlyInPackage) {
        if (!FST.isPresent()) {
            return new ArrayList<>();
        }
        return FSTUtils.getFSTEntriesByRegEx(FST.get().getRoot(), regEx, onlyInPackage);
    }

    public void cleanup() throws IOException {
        if (getDataProcessor() != null && getDataProcessor().getDataProvider() != null) {
            getDataProcessor().getDataProvider().cleanup();
        }
    }

    @Override
    public String toString() {
        return "NUSTitle [dataProcessor=" + dataProcessor + "]";
    }
}
