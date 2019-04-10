package de.mas.wiiu.jnus.implementations;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import de.mas.wiiu.jnus.entities.content.ContentFSTInfo;
import de.mas.wiiu.jnus.entities.fst.FSTEntry;
import de.mas.wiiu.jnus.implementations.wud.parser.WUDDataPartition;
import de.mas.wiiu.jnus.implementations.wud.reader.WUDDiscReader;
import de.mas.wiiu.jnus.interfaces.FSTDataProvider;

public class FSTDataProviderWUDDataPartition implements FSTDataProvider {
    private final WUDDataPartition partition;
    private final WUDDiscReader discReader;
    private final byte[] titleKey;

    public FSTDataProviderWUDDataPartition(WUDDataPartition partition, WUDDiscReader discReader, byte[] titleKey) {
        this.partition = partition;
        this.discReader = discReader;
        this.titleKey = titleKey;
    }

    @Override
    public String getName() {
        return partition.getPartitionName();
    }

    @Override
    public FSTEntry getRoot() {
        return partition.getFST().getRoot();
    }

    @Override
    public byte[] readFile(FSTEntry entry, long offset, long size) throws IOException {
        ContentFSTInfo info = partition.getFST().getContentFSTInfos().get((int) entry.getContentFSTID());
        return getChunkOfData(info.getOffset(), entry.getFileOffset() + offset, size, discReader, titleKey);
    }

    @Override
    public InputStream readFileAsStream(FSTEntry entry, long offset, Optional<Long> size) throws IOException {
        ContentFSTInfo info = partition.getFST().getContentFSTInfos().get((int) entry.getContentFSTID());
        long fileSize = size.orElse(entry.getFileSize());
        if (titleKey == null) {
            return discReader.readEncryptedToInputStream(partition.getAbsolutePartitionOffset() + info.getOffset() + entry.getFileOffset() + offset, fileSize);
        }
        return discReader.readDecryptedToInputStream(partition.getAbsolutePartitionOffset() + info.getOffset(), entry.getFileOffset() + offset, fileSize,
                titleKey, null, false);
    }

    public byte[] getChunkOfData(long contentOffset, long fileoffset, long size, WUDDiscReader discReader, byte[] titleKey) throws IOException {
        if (titleKey == null) {
            return discReader.readEncryptedToByteArray(partition.getAbsolutePartitionOffset() + contentOffset, fileoffset, (int) size);
        }
        return discReader.readDecryptedToByteArray(partition.getAbsolutePartitionOffset() + contentOffset, fileoffset, (int) size, titleKey, null, false);
    }

}
