package de.mas.wiiu.jnus.implementations.wud.parser;

import java.io.IOException;
import java.io.InputStream;

import de.mas.wiiu.jnus.Settings;
import de.mas.wiiu.jnus.entities.content.ContentFSTInfo;
import de.mas.wiiu.jnus.entities.fst.FST;
import de.mas.wiiu.jnus.entities.fst.FSTEntry;
import de.mas.wiiu.jnus.implementations.wud.reader.WUDDiscReader;
import de.mas.wiiu.jnus.utils.StreamUtils;
import lombok.Getter;

public class WUDGIPartitionTitle {
    private final FST fst;
    private final FSTEntry rootEntry;

    @Getter private final long partitionOffset;

    public WUDGIPartitionTitle(FST fst, FSTEntry rootEntry, long partitionOffset) {
        this.fst = fst;
        this.rootEntry = rootEntry;
        this.partitionOffset = partitionOffset;
    }

    public byte[] getFileAsByte(String filename, WUDDiscReader discReader, byte[] titleKey) throws IOException {
        FSTEntry entry = getEntryByFilename(rootEntry, filename);
        return StreamUtils.getBytesFromStream(getFileAsStream(filename, discReader, 0, titleKey), (int) entry.getFileSize());
    }

    public InputStream getFileAsStream(String filename, WUDDiscReader discReader, long offsetInFile, byte[] titleKey) throws IOException {
        FSTEntry entry = getEntryByFilename(rootEntry, filename);
        ContentFSTInfo info = fst.getContentFSTInfos().get((int) entry.getContentFSTID());

        return getFileAsStream(info.getOffset(), entry.getFileOffset() + offsetInFile, (int) entry.getFileSize(), discReader, titleKey);
    }

    public InputStream getFileAsStream(long contentOffset, long fileoffset, long size, WUDDiscReader discReader, byte[] titleKey) throws IOException {
        return discReader.readDecryptedToInputStream(getAbsoluteReadOffset() + contentOffset, fileoffset, (int) size, titleKey, null, false);
    }

    public byte[] getFileAsData(long contentOffset, long fileoffset, long size, WUDDiscReader discReader, byte[] titleKey) throws IOException {
        return discReader.readDecryptedToByteArray(getAbsoluteReadOffset() + contentOffset, fileoffset, (int) size, titleKey, null, false);
    }

    public byte[] getFileAsByte(String filename, WUDDiscReader discReader, long offsetInFile, int size, byte[] titleKey) throws IOException {
        FSTEntry entry = getEntryByFilename(rootEntry, filename);
        ContentFSTInfo info = fst.getContentFSTInfos().get((int) entry.getContentFSTID());

        return getFileAsData(info.getOffset(), entry.getFileOffset() + offsetInFile, size, discReader, titleKey);
    }

    private long getAbsoluteReadOffset() {
        return (long) Settings.WIIU_DECRYPTED_AREA_OFFSET + getPartitionOffset();
    }

    private static FSTEntry getEntryByFilename(FSTEntry root, String filename) {
        for (FSTEntry cur : root.getFileChildren()) {
            if (cur.getFilename().equalsIgnoreCase(filename)) {
                return cur;
            }
        }
        for (FSTEntry cur : root.getDirChildren()) {
            FSTEntry dir_result = getEntryByFilename(cur, filename);
            if (dir_result != null) {
                return dir_result;
            }
        }
        return null;
    }

}
