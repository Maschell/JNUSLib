package de.mas.jnus.lib.utils.download;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import de.mas.jnus.lib.Settings;
import lombok.Setter;

public class NUSDownloadService extends Downloader{
    private static NUSDownloadService defaultInstance;
    
    public static NUSDownloadService getDefaultInstance(){
        if(defaultInstance == null){
            defaultInstance = new NUSDownloadService();
            defaultInstance.setURL_BASE(Settings.URL_BASE);
        }
        return defaultInstance;
    }
    
    public static NUSDownloadService getInstance(String URL){
        NUSDownloadService instance = new NUSDownloadService();
        instance.setURL_BASE(URL);
        return instance;
    }
    
    private NUSDownloadService(){
        
    }
    
    @Setter private String URL_BASE = "";
    
    
    public byte[] downloadTMDToByteArray(long titleID, int version) throws IOException {
        String version_suf = "";
        if(version > Settings.LATEST_TMD_VERSION) version_suf = "." + version;
        String URL = URL_BASE + "/" + String.format("%016X", titleID) +  "/tmd" +version_suf;
        return downloadFileToByteArray(URL);
    }

    public byte[] downloadTicketToByteArray(long titleID) throws IOException {
        String URL = URL_BASE + "/" + String.format("%016X", titleID) +  "/cetk";
        return downloadFileToByteArray(URL);
    }

    public byte[] downloadToByteArray(String url) throws IOException {
        String URL = URL_BASE + "/" + url;
        return downloadFileToByteArray(URL);
    }
    
    public InputStream getInputStream(String URL,long offset) throws IOException{
        URL url_obj = new URL(URL);
        HttpURLConnection connection = (HttpURLConnection) url_obj.openConnection(); 
        connection.setRequestProperty("Range", "bytes=" + offset +"-"); 
        try{
            connection.connect();
        }catch(Exception e){
            e.printStackTrace();
        }

        return  connection.getInputStream();
    }

    public InputStream getInputStreamForURL(String url, long offset) throws IOException {
        String URL = URL_BASE + "/" + url;
        
        return getInputStream(URL,offset);
    }
}
