package de.mas.jnus.lib.implementations.woomy;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import de.mas.jnus.lib.utils.StreamUtils;
import lombok.Getter;
import lombok.Setter;

/**
 * Woomy files are just zip files. This class is just the
 * normal ZipFile class extended by an "isClosed" attribute.
 * @author Maschell
 *
 */
public class WoomyZipFile extends ZipFile {
    @Getter @Setter boolean isClosed;
    public WoomyZipFile(File file) throws ZipException, IOException {
        super(file);
    }
    
    @Override
    public void close() throws IOException {
        super.close();
        setClosed(true);
    }

    public byte[] getEntryAsByte(ZipEntry entry) throws IOException {
        return StreamUtils.getBytesFromStream(getInputStream(entry),(int) entry.getSize());
    }
    
}
