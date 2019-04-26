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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import de.mas.wiiu.jnus.entities.TMD;
import de.mas.wiiu.jnus.entities.Ticket;
import de.mas.wiiu.jnus.entities.fst.FST;
import de.mas.wiiu.jnus.entities.fst.FSTEntry;
import de.mas.wiiu.jnus.interfaces.NUSDataProvider;
import de.mas.wiiu.jnus.utils.FSTUtils;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class NUSTitle {
    @Getter @Setter private Optional<FST> FST = Optional.empty();
    @Getter @Setter private Optional<Ticket> ticket;

    @Getter private final TMD TMD;

    @Getter @Setter private boolean skipExistingFiles = true;
    @Getter private final NUSDataProvider dataProvider;

    public NUSTitle(@NonNull NUSDataProvider dataProvider) throws ParseException, IOException {
        byte[] tmdData = dataProvider.getRawTMD().orElseThrow(() -> new ParseException("No TMD data found", 0));
        this.TMD = de.mas.wiiu.jnus.entities.TMD.parseTMD(tmdData);
        this.dataProvider = dataProvider;
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
        if (getDataProvider() != null) {
            getDataProvider().cleanup();
        }
    }

    @Override
    public String toString() {
        return "NUSTitle [dataProvider=" + dataProvider + "]";
    }
}
