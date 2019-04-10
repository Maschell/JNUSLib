package de.mas.wiiu.jnus;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;

import de.mas.wiiu.jnus.entities.fst.FSTEntry;
import lombok.val;

public class FSTUtils {
    public static Optional<FSTEntry> getFSTEntryByFullPath(FSTEntry root, String givenFullPath) {
        String fullPath = givenFullPath.replace("/", File.separator);
        if (!fullPath.startsWith(File.separator)) {
            fullPath = File.separator + fullPath;
        }

        String dirPath = FilenameUtils.getFullPathNoEndSeparator(fullPath);
        Optional<FSTEntry> pathOpt = Optional.of(root);
        if (!dirPath.equals(File.separator)) {
            pathOpt = getFileEntryDir(root, dirPath);
        }

        String path = fullPath;

        return pathOpt.flatMap(e -> e.getChildren().stream().filter(c -> c.getFullPath().equals(path)).findAny());
    }

    public static Optional<FSTEntry> getFileEntryDir(FSTEntry curEntry, String string) {
        string = string.replace("/", File.separator);

        if (!string.endsWith(File.separator)) {
            string += File.separator;
        }
        for (val curChild : curEntry.getDirChildren()) {
            String compareTo = curChild.getFullPath();
            if (!compareTo.endsWith(File.separator)) {
                compareTo += File.separator;
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
        return getFSTEntriesByRegEx(string, root, false);
    }

    public static List<FSTEntry> getFSTEntriesByRegEx(String regEx, FSTEntry entry, boolean allowNotInPackage) {
        Pattern p = Pattern.compile(regEx);
        return getFSTEntriesByRegExStream(p, entry, allowNotInPackage).collect(Collectors.toList());
    }

    private static Stream<FSTEntry> getFSTEntriesByRegExStream(Pattern p, FSTEntry entry, boolean allowNotInPackage) {
        return entry.getChildren().stream()//
                .filter(e -> allowNotInPackage || !e.isNotInPackage()) //
                .flatMap(e -> {
                    if (!e.isDir()) {
                        if (p.matcher(e.getFullPath().replace("/", File.separator)).matches()) {
                            return Stream.of(e);
                        } else {
                            return Stream.empty();
                        }
                    }
                    return getFSTEntriesByRegExStream(p, e, allowNotInPackage);
                });
    }
}
