package de.mas.wiiu.jnus.implementations;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Optional;

import de.mas.wiiu.jnus.Settings;
import de.mas.wiiu.jnus.entities.content.Content;
import de.mas.wiiu.jnus.interfaces.NUSDataProvider;
import de.mas.wiiu.jnus.utils.StreamUtils;
import lombok.Getter;

public class NUSDataProviderLocalBackup implements NUSDataProvider {
    @Getter private final String localPath;
    private final short titleVersion;

    public NUSDataProviderLocalBackup(String localPath) {
        this(localPath, (short) Settings.LATEST_TMD_VERSION);
    }

    public NUSDataProviderLocalBackup(String localPath, short version) {
        this.localPath = localPath;
        this.titleVersion = version;
    }

    private String getFilePathOnDisk(Content c) {
        return getLocalPath() + File.separator + c.getFilename();
    }

    @Override
    public InputStream getInputStreamFromContent(Content content, long offset, Optional<Long> size) throws IOException {
        File filepath = new File(getFilePathOnDisk(content));
        if (!filepath.exists()) {
            throw new FileNotFoundException(filepath.getAbsolutePath() + " was not found.");
        }
        InputStream in = new FileInputStream(filepath);
        StreamUtils.skipExactly(in, offset);
        return in;
    }

    @Override
    public Optional<byte[]> getContentH3Hash(Content content) throws IOException {
        String h3Path = getLocalPath() + File.separator + String.format("%08X.h3", content.getID());
        File h3File = new File(h3Path);

        return Optional.of(Files.readAllBytes(h3File.toPath()));
    }

    @Override
    public Optional<byte[]> getRawTMD() throws IOException {
        String inputPath = getLocalPath();
        String tmdPath = inputPath + File.separator + Settings.TMD_FILENAME;
        if (titleVersion != Settings.LATEST_TMD_VERSION) {
            tmdPath = inputPath + File.separator + "v" + titleVersion + File.separator + Settings.TMD_FILENAME;
        }
        File tmdFile = new File(tmdPath);
        return Optional.of(Files.readAllBytes(tmdFile.toPath()));
    }

    @Override
    public Optional<byte[]> getRawTicket() throws IOException {
        String inputPath = getLocalPath();
        String ticketPath = inputPath + File.separator + Settings.TICKET_FILENAME;
        File ticketFile = new File(ticketPath);
        return Optional.of(Files.readAllBytes(ticketFile.toPath()));
    }

    @Override
    public Optional<byte[]> getRawCert() throws IOException {
        String inputPath = getLocalPath();
        String certPath = inputPath + File.separator + Settings.CERT_FILENAME;
        File certFile = new File(certPath);
        return Optional.of(Files.readAllBytes(certFile.toPath()));
    }

    @Override
    public void cleanup() throws IOException {
        // We don't need this
    }

    @Override
    public String toString() {
        String titleVersionString = titleVersion == Settings.LATEST_TMD_VERSION ? "latest" : Short.toString(titleVersion);
        return "NUSDataProviderLocalBackup [localPath=" + localPath + ", titleVersion=" + titleVersionString + "]";
    }

}
