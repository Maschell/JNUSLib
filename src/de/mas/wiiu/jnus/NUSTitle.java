package de.mas.wiiu.jnus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.mas.wiiu.jnus.entities.TMD;
import de.mas.wiiu.jnus.entities.Ticket;
import de.mas.wiiu.jnus.entities.content.Content;
import de.mas.wiiu.jnus.entities.content.ContentFSTInfo;
import de.mas.wiiu.jnus.entities.fst.FST;
import de.mas.wiiu.jnus.entities.fst.FSTEntry;
import de.mas.wiiu.jnus.implementations.NUSDataProvider;
import lombok.Getter;
import lombok.Setter;

public class NUSTitle {
    @Getter @Setter private FST FST;
    @Getter @Setter private TMD TMD;
    @Getter @Setter private Ticket ticket;

    @Getter @Setter private boolean skipExistingFiles = true;
    @Getter @Setter private NUSDataProvider dataProvider = null;

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
        List<FSTEntry> entries = new ArrayList<>();
        for (Content c : list) {
            for (FSTEntry f : c.getEntries()) {
                entries.add(f);
            }
        }
        return entries;
    }

    public List<FSTEntry> getAllFSTEntriesFlat() {
        return getFSTEntriesFlatByContents(new ArrayList<Content>(getTMD().getAllContents().values()));
    }

    public FSTEntry getFSTEntryByFullPath(String givenFullPath) {
        String fullPath = givenFullPath.replaceAll("/", "\\\\");
        if (!fullPath.startsWith("\\")) fullPath = "\\" + fullPath;
        for (FSTEntry f : getAllFSTEntriesFlat()) {
            if (f.getFullPath().equals(fullPath)) {
                return f;
            }
        }
        return null;
    }
    
    public List<FSTEntry> getFSTEntriesByRegEx(String regEx) {
        List<FSTEntry> files = getAllFSTEntriesFlat();
        Pattern p = Pattern.compile(regEx);
    
        List<FSTEntry> result = new ArrayList<>();
    
        for (FSTEntry f : files) {
            String match = f.getFullPath().replaceAll("\\\\", "/");
            Matcher m = p.matcher(match);
            if (m.matches()) {
                result.add(f);
            }
        }
        return result;
    }

    public void printFiles() {
        getFST().getRoot().printRecursive(0);
    }

    public void printContentFSTInfos() {
        for (Entry<Integer, ContentFSTInfo> e : getFST().getContentFSTInfos().entrySet()) {
            System.out.println(String.format("%08X", e.getKey()) + ": " + e.getValue());
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
