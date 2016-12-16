package de.mas.jnus.lib;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mas.jnus.lib.entities.content.Content;
import de.mas.jnus.lib.implementations.NUSDataProvider;
import de.mas.jnus.lib.utils.FileUtils;
import de.mas.jnus.lib.utils.Utils;
import lombok.Getter;

public final class ExtractionService {
    private static Map<NUSTitle,ExtractionService> instances = new HashMap<>();
    
    @Getter private final NUSTitle NUSTitle;
    
    public static ExtractionService getInstance(NUSTitle nustitle) {
        if(!instances.containsKey(nustitle)){
            instances.put(nustitle, new ExtractionService(nustitle));
        }
        return instances.get(nustitle);
    }

    private ExtractionService(NUSTitle nustitle){
        this.NUSTitle = nustitle;
    }
    
    private NUSDataProvider getDataProvider(){
        return getNUSTitle().getDataProvider();
    }
    
    public void extractAllEncrpytedContentFileHashes(String outputFolder) throws IOException {
        extractEncryptedContentHashesTo(new ArrayList<Content>(getNUSTitle().getTMD().getAllContents().values()),outputFolder);
    }
    
    public void extractEncryptedContentHashesTo(List<Content> list, String outputFolder) throws IOException {
        Utils.createDir(outputFolder);
        NUSDataProvider dataProvider = getDataProvider();
        for(Content c : list){
            dataProvider.saveContentH3Hash(c, outputFolder);
        }
    }

    public void extractAllEncryptedContentFiles(String outputFolder) throws IOException {
        extractAllEncryptedContentFilesWithHashesTo(outputFolder);
    }
    
    public void extractAllEncryptedContentFilesWithoutHashesTo(String outputFolder) throws IOException {
        extractEncryptedContentFilesTo(new ArrayList<Content>(getNUSTitle().getTMD().getAllContents().values()),outputFolder,false);
    }
    
    public void extractAllEncryptedContentFilesWithHashesTo(String outputFolder) throws IOException {
        extractEncryptedContentFilesTo(new ArrayList<Content>(getNUSTitle().getTMD().getAllContents().values()),outputFolder,true);
    }

    public void extractEncryptedContentFilesTo(List<Content> list,String outputFolder,boolean withHashes) throws IOException {
        Utils.createDir(outputFolder);
        NUSDataProvider dataProvider = getDataProvider();
        for(Content c : list){
            System.out.println("Saving " + c.getFilename());
            if(withHashes){
                dataProvider.saveEncryptedContentWithH3Hash(c, outputFolder);
            }else{
                dataProvider.saveEncryptedContent(c, outputFolder);
            }
        }
    }

    public void extractTMDTo(String output) throws IOException {
        Utils.createDir(output);
        
        byte[] rawTMD=  getDataProvider().getRawTMD();
        
        if(rawTMD == null || rawTMD.length == 0){
            System.out.println("Couldn't write TMD: No TMD loaded");
            return;
        }
        String tmd_path = output + File.separator + Settings.TMD_FILENAME;
        System.out.println("Extracting TMD to: " + tmd_path);
        FileUtils.saveByteArrayToFile(tmd_path,rawTMD);
    }

    public void extractTicketTo(String output) throws IOException {
        Utils.createDir(output);
        
        byte[] rawTicket=  getDataProvider().getRawTicket();
        
        if(rawTicket == null || rawTicket.length == 0){
            System.out.println("Couldn't write Ticket: No Ticket loaded");
            return;
        }
        String ticket_path = output + File.separator + Settings.TICKET_FILENAME;
        System.out.println("Extracting Ticket to: " + ticket_path);
        FileUtils.saveByteArrayToFile(ticket_path,rawTicket);
    }
    
    public void extractCertTo(String output) throws IOException {
        Utils.createDir(output);
        
        byte[] rawCert =  getDataProvider().getRawCert();
        
        if(rawCert == null || rawCert.length == 0){
            System.out.println("Couldn't write Cert: No Cert loaded");
            return;
        }
        String cert_path = output + File.separator + Settings.CERT_FILENAME;
        System.out.println("Extracting Cert to: " + cert_path);
        FileUtils.saveByteArrayToFile(cert_path,rawCert);
    }

    public void extractAll(String outputFolder) throws IOException {
        Utils.createDir(outputFolder);
        extractAllEncryptedContentFilesWithHashesTo(outputFolder);
        extractCertTo(outputFolder);
        extractTMDTo(outputFolder);
        extractTicketTo(outputFolder);
        
    }

}
