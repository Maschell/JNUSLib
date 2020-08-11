package de.mas.wiiu.jnus.implementations;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import de.mas.wiiu.jnus.entities.FST.nodeentry.FileEntry;
import de.mas.wiiu.jnus.entities.FST.nodeentry.RootEntry;
import de.mas.wiiu.jnus.implementations.wud.wumad.WumadDataPartition;
import de.mas.wiiu.jnus.interfaces.FSTDataProvider;
import de.mas.wiiu.jnus.utils.StreamUtils;

public class FSTDataProviderWumadDataPartition implements FSTDataProvider {
    private final WumadDataPartition dataPartition;
    private final ZipFile zipFile;

    public FSTDataProviderWumadDataPartition(WumadDataPartition dataPartition, ZipFile zipFile) {
        this.dataPartition = dataPartition;
        this.zipFile = zipFile;
    }

    @Override
    public String getName() {
        return dataPartition.getPartitionName();
    }

    @Override
    public RootEntry getRoot() {
        return dataPartition.getFST().getRootEntry();
    }

    @Override
    public long readFileToStream(OutputStream out, FileEntry entry, long offset, long size) throws IOException {
        return StreamUtils.saveInputStreamToOutputStream(readFileAsStream(entry, offset, size), out, size);
    }

    @Override
    public InputStream readFileAsStream(FileEntry entry, long offset, long size) throws IOException {
        ZipEntry zipEntry = zipFile.stream().filter(
                e -> e.getName().equals(String.format("p%s.s%04d.00000000.app", dataPartition.getPartitionName(), entry.getSectionEntry().getSectionNumber())))
                .findFirst().orElseThrow(() -> new FileNotFoundException());

        InputStream in = zipFile.getInputStream(zipEntry);
        StreamUtils.skipExactly(in, offset + entry.getOffset());
        return in;
    }

}
