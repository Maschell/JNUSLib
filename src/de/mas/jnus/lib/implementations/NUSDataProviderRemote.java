package de.mas.jnus.lib.implementations;

import java.io.IOException;
import java.io.InputStream;

import de.mas.jnus.lib.Settings;
import de.mas.jnus.lib.entities.content.Content;
import de.mas.jnus.lib.utils.download.NUSDownloadService;
import lombok.Getter;
import lombok.Setter;

public class NUSDataProviderRemote extends NUSDataProvider {
    @Getter @Setter private int version = Settings.LATEST_TMD_VERSION;
    @Getter @Setter private long titleID = 0L;
    
    @Override
    public InputStream getInputStreamFromContent(Content content, long fileOffsetBlock) throws IOException {
        NUSDownloadService downloadService = NUSDownloadService.getDefaultInstance();
        InputStream in = downloadService.getInputStreamForURL(getRemoteURL(content),fileOffsetBlock);
        return in;
    }

    private String getRemoteURL(Content content) {        
        return String.format("%016x/%08X", getNUSTitle().getTMD().getTitleID(),content.getID());
    }

    @Override
    public byte[] getContentH3Hash(Content content) throws IOException {
        NUSDownloadService downloadService = NUSDownloadService.getDefaultInstance();
        String url = getRemoteURL(content) + ".h3";
        return downloadService.downloadToByteArray(url);
    }

    @Override
    public byte[] getRawTMD() throws IOException {
        NUSDownloadService downloadService = NUSDownloadService.getDefaultInstance();
        
        long titleID = getTitleID();
        int version = getVersion();
        
        return downloadService.downloadTMDToByteArray(titleID, version);
    }

    @Override
    public byte[] getRawTicket() throws IOException {
        NUSDownloadService downloadService = NUSDownloadService.getDefaultInstance();
        
        long titleID = getNUSTitle().getTMD().getTitleID();
        
        return downloadService.downloadTicketToByteArray(titleID);
    }

    @Override
    public void cleanup() throws IOException {
        // TODO Auto-generated method stub
    }

    @Override
    public byte[] getRawCert() throws IOException {
        return null;
    }
}
