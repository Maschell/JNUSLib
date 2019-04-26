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
package de.mas.wiiu.jnus.utils;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;

import de.mas.wiiu.jnus.entities.content.ContentFSTInfo;
import de.mas.wiiu.jnus.entities.fst.FST;
import de.mas.wiiu.jnus.entities.fst.FSTEntry;
import lombok.val;

public class FSTUtils {
    public static Optional<FSTEntry> getFSTEntryByFullPath(FSTEntry root, String givenFullPath) {
        String fullPath = givenFullPath.replace(File.separator, "/");
        if (!fullPath.startsWith("/")) {
            fullPath = "/" + fullPath;
        }

        String dirPath = FilenameUtils.getFullPathNoEndSeparator(fullPath);
        Optional<FSTEntry> pathOpt = Optional.of(root);
        if (!dirPath.equals("/")) {
            pathOpt = getFileEntryDir(root, dirPath);
        }

        String path = fullPath;

        return pathOpt.flatMap(e -> e.getChildren().stream().filter(c -> c.getFullPath().equals(path)).findAny());
    }

    public static Optional<FSTEntry> getFileEntryDir(FSTEntry curEntry, String string) {
        string = string.replace(File.separator, "/");

        // We add the "/" at the end so we don't get false results when using the "startWith" function.
        if (!string.endsWith("/")) {
            string += "/";
        }
        for (val curChild : curEntry.getDirChildren()) {
            String compareTo = curChild.getFullPath();
            if (!compareTo.endsWith("/")) {
                compareTo += "/";
            }
            if (string.startsWith(compareTo)) {
                if (string.equals(compareTo)) {
                    return Optional.of(curChild);
                }
                return getFileEntryDir(curChild, string);
            }
        }

        return Optional.empty();
    }

    public static Optional<FSTEntry> getEntryByFullPath(FSTEntry root, String filePath) {
        for (FSTEntry cur : root.getFileChildren()) {
            if (cur.getFullPath().equals(filePath)) {
                return Optional.of(cur);
            }
        }

        for (FSTEntry cur : root.getDirChildren()) {
            Optional<FSTEntry> res = getEntryByFullPath(cur, filePath);
            if (res.isPresent()) {
                return res;
            }
        }
        return Optional.empty();
    }

    public static Optional<FSTEntry> getChildOfDirectory(FSTEntry root, String filename) {
        for (FSTEntry cur : root.getChildren()) {
            if (cur.getFilename().equalsIgnoreCase(filename)) {
                return Optional.of(cur);
            }
        }
        return Optional.empty();
    }

    public static List<FSTEntry> getFSTEntriesByRegEx(FSTEntry root, String string) {
        return getFSTEntriesByRegEx(root, string, false);
    }

    public static List<FSTEntry> getFSTEntriesByRegEx(FSTEntry entry, String regEx, boolean allowNotInPackage) {
        Pattern p = Pattern.compile(regEx);
        return getFSTEntriesByRegExStream(entry, p, allowNotInPackage).collect(Collectors.toList());
    }

    private static Stream<FSTEntry> getFSTEntriesByRegExStream(FSTEntry entry, Pattern p, boolean allowNotInPackage) {
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
                    return getFSTEntriesByRegExStream(e, p, allowNotInPackage);
                });
    }

    public static Optional<ContentFSTInfo> getFSTInfoForContent(FST fst, short contentIndex) {
        return fst.getContentFSTInfos().entrySet().stream().filter(e -> e.getKey().shortValue() == contentIndex).map(e -> e.getValue()).findAny();
    }

    public static List<FSTEntry> getFSTEntriesByContentIndex(FSTEntry entry, short index) {
        return getFSTEntriesByContentIndexAsStream(entry, index).collect(Collectors.toList());
    }

    public static Stream<FSTEntry> getFSTEntriesByContentIndexAsStream(FSTEntry entry, short index) {
        return entry.getChildren().stream()//
                .filter(e -> e.getContentIndex() == index) //
                .flatMap(e -> {
                    if (!e.isDir()) {
                        return Stream.of(e);
                    }
                    return getFSTEntriesByContentIndexAsStream(e, index);
                });
    }

    /**
     * Does not include entries that are not in the package..
     */
    public static Stream<FSTEntry> getAllFSTEntryChildrenAsStream(FSTEntry root) {
        return getAllFSTEntryChildrenAsStream(root, false);
    }

    public static Stream<FSTEntry> getAllFSTEntryChildrenAsStream(FSTEntry root, boolean allowNotInPackage) {
        return root.getChildren().stream() //
                .filter(e -> allowNotInPackage || !e.isNotInPackage()) //
                .flatMap(e -> {
                    if (!e.isDir()) {
                        return Stream.of(e);
                    }
                    return getAllFSTEntryChildrenAsStream(e, allowNotInPackage);
                });
    }
}
