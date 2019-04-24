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

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.mas.wiiu.jnus.entities.TMD;
import de.mas.wiiu.jnus.entities.Ticket;
import de.mas.wiiu.jnus.entities.content.Content;
import de.mas.wiiu.jnus.entities.content.ContentFSTInfo;
import de.mas.wiiu.jnus.entities.fst.FST;
import de.mas.wiiu.jnus.entities.fst.FSTEntry;
import de.mas.wiiu.jnus.interfaces.NUSDataProvider;
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

    public List<FSTEntry> getAllFSTEntriesFlatByContentID(short ID) {
        return getFSTEntriesFlatByContent(getTMD().getContentByID((int) ID));
    }

    public List<FSTEntry> getFSTEntriesFlatByContentIndex(int index) {
        return getFSTEntriesFlatByContent(getTMD().getContentByIndex(index));
    }

    public List<FSTEntry> getFSTEntriesFlatByContent(Content content) {
        return getFSTEntriesFlatByContents(new ArrayList<Content>(Arrays.asList(content)));
    }

    public List<FSTEntry> getFSTEntriesFlatByContents(List<Content> list) {
        return list.stream().flatMap(c -> c.getEntries().stream()).collect(Collectors.toList());
    }

    public List<FSTEntry> getAllFSTEntriesFlat() {
        return getFSTEntriesFlatByContents(new ArrayList<Content>(getTMD().getAllContents().values()));
    }

    public Stream<FSTEntry> getAllFSTEntriesAsStream() {
        if (!FST.isPresent()) {
            return Stream.empty();
        }
        return getAllFSTEntryChildrenAsStream(FST.get().getRoot());
    }

    public Stream<FSTEntry> getAllFSTEntryChildrenAsStream(FSTEntry cur) {
        return getAllFSTEntryChildrenAsStream(cur, false);
    }

    public Stream<FSTEntry> getAllFSTEntryChildrenAsStream(FSTEntry cur, boolean allowNotInPackage) {
        return cur.getChildren().stream() //
                .filter(e -> allowNotInPackage || !e.isNotInPackage()) //
                .flatMap(e -> {
                    if (!e.isDir()) {
                        return Stream.of(e);
                    }
                    return getAllFSTEntryChildrenAsStream(e, allowNotInPackage);
                });
    }

    public List<FSTEntry> getFSTEntriesByRegEx(String regEx) {
        if (!FST.isPresent()) {
            return new ArrayList<>();
        }

        return getFSTEntriesByRegEx(regEx, FST.get().getRoot());
    }

    public List<FSTEntry> getFSTEntriesByRegEx(String regEx, FSTEntry entry) {
        return getFSTEntriesByRegEx(regEx, entry, true);
    }

    public List<FSTEntry> getFSTEntriesByRegEx(String regEx, boolean onlyInPackage) {
        if (!FST.isPresent()) {
            return new ArrayList<>();
        }
        return getFSTEntriesByRegEx(regEx, FST.get().getRoot(), onlyInPackage);
    }

    public List<FSTEntry> getFSTEntriesByRegEx(String regEx, FSTEntry entry, boolean allowNotInPackage) {
        Pattern p = Pattern.compile(regEx);
        return getFSTEntriesByRegExStream(p, entry, allowNotInPackage).collect(Collectors.toList());
    }

    private Stream<FSTEntry> getFSTEntriesByRegExStream(Pattern p, FSTEntry entry, boolean allowNotInPackage) {
        return entry.getChildren().stream()//
                .filter(e -> allowNotInPackage || !e.isNotInPackage()) //
                .flatMap(e -> {
                    if (!e.isDir()) {
                        if (p.matcher(e.getFullPath()).matches()) {
                            return Stream.of(e);
                        } else {
                            return Stream.empty();
                        }
                    }
                    return getFSTEntriesByRegExStream(p, e, allowNotInPackage);
                });
    }

    public void printFiles() {
        if (FST.isPresent()) {
            FST.get().getRoot().printRecursive(0);
        }
    }

    public void printContentFSTInfos() {
        if (FST.isPresent()) {
            for (Entry<Integer, ContentFSTInfo> e : FST.get().getContentFSTInfos().entrySet()) {
                System.out.println(String.format("%08X", e.getKey()) + ": " + e.getValue());
            }
        }

    }

    public void printContentInfos() {
        for (Entry<Integer, Content> e : getTMD().getAllContents().entrySet()) {

            System.out.println(String.format("%08X", e.getKey()) + ": " + e.getValue());
            System.out.println(e.getValue().getContentFSTInfo());
            for (FSTEntry entry : e.getValue().getEntries()) {
                System.out.println(entry.getFullPath() + String.format(" size: %016X", entry.getFileSize())
                        + String.format(" offset: %016X", entry.getFileOffset()) + String.format(" flags: %04X", entry.getFlags()));
            }
            System.out.println("-");
        }
    }

    public void cleanup() throws IOException {
        if (getDataProvider() != null) {
            getDataProvider().cleanup();
        }
    }

    public void printDetailedData() {
        printFiles();
        printContentFSTInfos();
        printContentInfos();

        System.out.println();
    }

    @Override
    public String toString() {
        return "NUSTitle [dataProvider=" + dataProvider + "]";
    }
}
