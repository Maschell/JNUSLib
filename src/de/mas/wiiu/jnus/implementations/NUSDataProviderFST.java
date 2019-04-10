package de.mas.wiiu.jnus.implementations;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import de.mas.wiiu.jnus.Settings;
import de.mas.wiiu.jnus.entities.content.Content;
import de.mas.wiiu.jnus.entities.fst.FSTEntry;
import de.mas.wiiu.jnus.interfaces.FSTDataProvider;
import de.mas.wiiu.jnus.interfaces.NUSDataProvider;
import de.mas.wiiu.jnus.utils.FSTUtils;

public class NUSDataProviderFST implements NUSDataProvider {
    private final FSTDataProvider fstDataProvider;
    private final FSTEntry base;

    public NUSDataProviderFST(FSTDataProvider fstDataProvider, FSTEntry base) {
        this.base = base;
        this.fstDataProvider = fstDataProvider;
    }

    public NUSDataProviderFST(FSTDataProvider fstDataProvider) {
        this(fstDataProvider, fstDataProvider.getRoot());
    }

    @Override
    public InputStream getInputStreamFromContent(Content content, long offset, Optional<Long> size) throws IOException {
        String filename = content.getFilename();
        Optional<FSTEntry> contentFileOpt = FSTUtils.getChildOfDirectory(base, filename);
        FSTEntry contentFile = contentFileOpt.orElseThrow(() -> new FileNotFoundException(filename + " was not found."));
        return fstDataProvider.readFileAsStream(contentFile, offset, size);
    }

    @Override
    public Optional<byte[]> getContentH3Hash(Content content) throws IOException {
        return readFileByFilename(base, String.format("%08X%s", content.getID(), Settings.H3_EXTENTION));
    }

    private Optional<byte[]> readFileByFilename(FSTEntry base, String filename) throws IOException {
        Optional<FSTEntry> entryOpt = FSTUtils.getChildOfDirectory(base, filename);
        if (entryOpt.isPresent()) {

            FSTEntry entry = entryOpt.get();
            return Optional.of(fstDataProvider.readFile(entry));
        }
        return Optional.empty();
    }

    @Override
    public Optional<byte[]> getRawTMD() throws IOException {
        return readFileByFilename(base, Settings.TMD_FILENAME);
    }

    @Override
    public Optional<byte[]> getRawTicket() throws IOException {
        return readFileByFilename(base, Settings.TICKET_FILENAME);
    }

    @Override
    public Optional<byte[]> getRawCert() throws IOException {
        return readFileByFilename(base, Settings.CERT_FILENAME);
    }

    @Override
    public void cleanup() throws IOException {

    }

}
