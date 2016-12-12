package de.mas.jnus.lib.implementations.woomy;

import java.io.File;
import java.util.Map;
import java.util.zip.ZipEntry;

import lombok.Data;

@Data
public class WoomyInfo {
    private String name;
    private File woomyFile;
    private Map<String,ZipEntry> contentFiles;
}
