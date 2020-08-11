package de.mas.wiiu.jnus.entities.FST.nodeentry;

import java.io.File;

import lombok.Getter;

public class RealFileEntry extends FileEntry {
    @Getter private final File fd;

    public RealFileEntry(File inputFile) {
        super();
        this.fd = inputFile;
        this.size = inputFile.length();
    }

}
