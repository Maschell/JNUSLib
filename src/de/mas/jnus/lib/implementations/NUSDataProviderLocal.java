package de.mas.jnus.lib.implementations;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import de.mas.jnus.lib.Settings;
import de.mas.jnus.lib.entities.content.Content;
import lombok.Getter;
import lombok.Setter;

public class NUSDataProviderLocal extends NUSDataProvider {
    @Getter @Setter private String localPath = "";
    
    public NUSDataProviderLocal() {
    }
    
    public String getFilePathOnDisk(Content c) {
        return getLocalPath() + File.separator + c.getFilename();
    }
    
    @Override
    public InputStream getInputStreamFromContent(Content content, long offset) throws IOException {
        File filepath = new File(getFilePathOnDisk(content));
        if(!filepath.exists()){
            
            return null;
        }
        InputStream in = new FileInputStream(filepath);
        in.skip(offset);
        return in;
    }

    @Override
    public byte[] getContentH3Hash(Content content) throws IOException {
        String h3Path = getLocalPath() + File.separator + String.format("%08X.h3", content.getID());
        File h3File = new File(h3Path);
        if(!h3File.exists()){   
            return null;
        }
        return Files.readAllBytes(h3File.toPath());
    }

    @Override
    public byte[] getRawTMD() throws IOException {
        String inputPath = getLocalPath();
        String tmdPath = inputPath + File.separator + Settings.TMD_FILENAME;
        File tmdFile = new File(tmdPath);
        return Files.readAllBytes(tmdFile.toPath());
    }

    @Override
    public byte[] getRawTicket() throws IOException { 
        String inputPath = getLocalPath();
        String ticketPath = inputPath + File.separator + Settings.TICKET_FILENAME;
        File ticketFile = new File(ticketPath);        
        return Files.readAllBytes(ticketFile.toPath());
    }

    @Override
    public void cleanup() throws IOException {
    }

    @Override
    public byte[] getRawCert() throws IOException {
        String inputPath = getLocalPath();
        String certPath = inputPath + File.separator + Settings.CERT_FILENAME;
        File certFile = new File(certPath);        
        return Files.readAllBytes(certFile.toPath());
    }
}
