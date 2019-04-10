package de.mas.wiiu.jnus.interfaces;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import de.mas.wiiu.jnus.entities.fst.FSTEntry;

public interface FSTDataProvider {
    public String getName();

    public FSTEntry getRoot();

    public default byte[] readFile(FSTEntry entry) throws IOException {
        return readFile(entry, 0, entry.getFileSize());
    }

    public byte[] readFile(FSTEntry entry, long offset, long size) throws IOException;

    public InputStream readFileAsStream(FSTEntry entry, long offset, Optional<Long> size) throws IOException;

}
