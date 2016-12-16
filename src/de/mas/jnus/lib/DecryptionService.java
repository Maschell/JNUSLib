package de.mas.jnus.lib;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.mas.jnus.lib.entities.TMD;
import de.mas.jnus.lib.entities.Ticket;
import de.mas.jnus.lib.entities.content.Content;
import de.mas.jnus.lib.entities.fst.FSTEntry;
import de.mas.jnus.lib.implementations.NUSDataProvider;
import de.mas.jnus.lib.utils.HashUtil;
import de.mas.jnus.lib.utils.StreamUtils;
import de.mas.jnus.lib.utils.Utils;
import de.mas.jnus.lib.utils.cryptography.NUSDecryption;
import lombok.Getter;

public final class DecryptionService {
    private static Map<NUSTitle,DecryptionService> instances = new HashMap<>();
    @Getter private final NUSTitle NUSTitle;
    
    public static DecryptionService getInstance(NUSTitle nustitle) {
        if(!instances.containsKey(nustitle)){
            instances.put(nustitle, new DecryptionService(nustitle));
        }
        return instances.get(nustitle);
    }
    
    private DecryptionService(NUSTitle nustitle){
        this.NUSTitle = nustitle;
    }
    
    public Ticket getTicket() {
        return getNUSTitle().getTicket();
    }

    public void decryptFSTEntryTo(boolean useFullPath,FSTEntry entry, String outputPath, boolean skipExistingFile) throws IOException {        
        if(entry.isNotInPackage() || entry.getContent() == null){
            return;
        }
        
        String targetFilePath = new StringBuilder().append(outputPath).append("/").append(entry.getFilename()).toString();
        String fullPath =  new StringBuilder().append(outputPath).toString();    
       
        if(useFullPath){
            targetFilePath =  new StringBuilder().append(outputPath).append(entry.getFullPath()).toString();            
            fullPath =  new StringBuilder().append(outputPath).append(entry.getPath()).toString(); 
            if(entry.isDir()){ //If the entry is a directory. Create it and return.
                Utils.createDir(targetFilePath);
                return;
            }
        }else if(entry.isDir()){
            return;
        }
        
        if(!Utils.createDir(fullPath)){
            return;
        }
        
        File target = new File(targetFilePath);
        
        if(skipExistingFile){
            File targetFile = new File(targetFilePath);
            if(targetFile.exists()){
                if(entry.isDir()){
                    return;
                }
                if(targetFile.length() == entry.getFileSize()){   
                    Content c = entry.getContent();
                    if(c.isHashed()){
                        System.out.println("File already exists: " + entry.getFilename());
                        return;
                    }else{
                        if(Arrays.equals(HashUtil.hashSHA1(target,(int) c.getEncryptedFileSize()), c.getSHA2Hash())){
                            System.out.println("File already exists: " + entry.getFilename());
                            return;
                        }else{
                            System.out.println("File already exists with the same filesize, but the hash doesn't match: " + entry.getFilename());
                        }
                    }
                  
                }else{
                    System.out.println("File already exists but the filesize doesn't match: " + entry.getFilename());
                }
            }
        }
        
        FileOutputStream outputStream = new FileOutputStream(new File(targetFilePath));
      
        System.out.println("Decrypting " + entry.getFilename());
        decryptFSTEntryToStream(entry, outputStream);
    }
    
    public void decryptFSTEntryToStream(FSTEntry entry, OutputStream outputStream) throws IOException {
        if(entry.isNotInPackage() || entry.getContent() == null){
            return;
        }
        
        Content c = entry.getContent();
        
        long fileSize = entry.getFileSize();
        long fileOffset = entry.getFileOffset();
        long fileOffsetBlock = entry.getFileOffsetBlock();
        
        NUSDataProvider dataProvider = getNUSTitle().getDataProvider();
        InputStream in = dataProvider.getInputStreamFromContent(c, fileOffsetBlock);
        
        decryptFSTEntryFromStreams(in,outputStream,fileSize,fileOffset,c);
    }
    
    
    private void decryptFSTEntryFromStreams(InputStream inputStream, OutputStream outputStream,long filesize, long fileoffset, Content content) throws IOException{
        decryptStreams(inputStream, outputStream, filesize, fileoffset, content);
    }
    
    private void decryptContentFromStream(InputStream inputStream, OutputStream outputStream,Content content) throws IOException { 
        long filesize = content.getDecryptedFileSize();
        System.out.println("Decrypting Content " + String.format("%08X", content.getID()));
        decryptStreams(inputStream, outputStream, filesize, 0L, content);
    }
    
    
    private void decryptStreams(InputStream inputStream, OutputStream outputStream,long size, long offset, Content content) throws IOException{
        NUSDecryption nusdecryption = new NUSDecryption(getTicket());
        short contentIndex = (short)content.getIndex();
        
        long encryptedFileSize = content.getEncryptedFileSize();
        if(content.isEncrypted()){
            if(content.isHashed()){
                NUSDataProvider dataProvider = getNUSTitle().getDataProvider();
                byte[] h3 = dataProvider.getContentH3Hash(content);
                nusdecryption.decryptFileStreamHashed(inputStream, outputStream, size, offset, (short) contentIndex, h3);
            }else{
                nusdecryption.decryptFileStream(inputStream, outputStream, size, (short)contentIndex,content.getSHA2Hash(),encryptedFileSize);
            }
        }else{
            StreamUtils.saveInputStreamToOutputStreamWithHash(inputStream, outputStream,size,content.getSHA2Hash(),encryptedFileSize);
        }
       
        inputStream.close();
        outputStream.close();
    }
       

    public void decryptContentTo(Content content,String outPath,boolean skipExistingFile) throws IOException {
        String targetFilePath = outPath + File.separator + content.getFilenameDecrypted();
        if(skipExistingFile){
            File targetFile = new File(targetFilePath);
            if(targetFile.exists()){
                if(targetFile.length() == content.getDecryptedFileSize()){
                    System.out.println("File already exists : " + content.getFilenameDecrypted());
                    return;
                }else{
                    System.out.println("File already exists but the filesize doesn't match: " +content.getFilenameDecrypted());
                }
            }
        }
        
        if(!Utils.createDir(outPath)){
            return;
        }
        
        System.out.println("Decrypting Content " + String.format("%08X", content.getID()));
        
        FileOutputStream outputStream = new FileOutputStream(new File(targetFilePath));
        
        decryptContentToStream(content, outputStream);
    }
    
    
    public void decryptContentToStream(Content content, OutputStream outputStream) throws IOException {
        if(content == null){
            return;
        }
        
        NUSDataProvider dataProvider = getNUSTitle().getDataProvider();
        InputStream inputStream = dataProvider.getInputStreamFromContent(content, 0);
        
        decryptContentFromStream(inputStream,outputStream,content);
    }

    public InputStream getDecryptedOutputAsInputStream(FSTEntry fstEntry) throws IOException {                
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = new PipedOutputStream(in);
        
        new Thread(() -> {try {
                decryptFSTEntryToStream(fstEntry, out);
        } catch (IOException e) {e.printStackTrace();}}).start();
        
        return in;        
    }
    public InputStream getDecryptedContentAsInputStream(Content content) throws IOException {                
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = new PipedOutputStream(in);
                
        new Thread(() -> {try {
                decryptContentToStream(content, out);
            } catch (IOException e) {e.printStackTrace();}}).start();
        
        return in;        
    }
    
    
    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //Decrypt FSTEntry to OutputStream
    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    public void decryptFSTEntryTo(String entryFullPath,OutputStream outputStream) throws IOException{
        FSTEntry entry = getNUSTitle().getFSTEntryByFullPath(entryFullPath);
        if(entry == null){
            System.out.println("File not found");
        }
        
        decryptFSTEntryToStream(entry, outputStream);
    }
    
    
    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //Decrypt single FSTEntry to File
    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    public void decryptFSTEntryTo(String entryFullPath,String outputFolder) throws IOException{
        decryptFSTEntryTo(false,entryFullPath,outputFolder);
    }
    
    public void decryptFSTEntryTo(boolean fullPath,String entryFullPath,String outputFolder) throws IOException{
        decryptFSTEntryTo(fullPath,entryFullPath,outputFolder,getNUSTitle().isSkipExistingFiles());
    }
    
    public void decryptFSTEntryTo(String entryFullPath,String outputFolder, boolean skipExistingFiles) throws IOException{
        decryptFSTEntryTo(false,entryFullPath,outputFolder,getNUSTitle().isSkipExistingFiles());
    }
    
    public void decryptFSTEntryTo(boolean fullPath, String entryFullPath,String outputFolder, boolean skipExistingFiles) throws IOException{
        FSTEntry entry = getNUSTitle().getFSTEntryByFullPath(entryFullPath);
        if(entry == null){
            System.out.println("File not found");
            return;
        }
        
        decryptFSTEntryTo(fullPath,entry, outputFolder,skipExistingFiles);
    }
    
    public void decryptFSTEntryTo(FSTEntry entry,String outputFolder) throws IOException{
        decryptFSTEntryTo(false,entry, outputFolder);
    }
    public void decryptFSTEntryTo(boolean fullPath,FSTEntry entry,String outputFolder) throws IOException{
        decryptFSTEntryTo(fullPath,entry,outputFolder,getNUSTitle().isSkipExistingFiles());
    }
    
    public void decryptFSTEntryTo(FSTEntry entry,String outputFolder, boolean skipExistingFiles) throws IOException{
        decryptFSTEntryTo(false,entry,outputFolder,getNUSTitle().isSkipExistingFiles());
    }
    
    /*
    public void decryptFSTEntryTo(boolean fullPath, FSTEntry entry,String outputFolder, boolean skipExistingFiles) throws IOException{
        decryptFSTEntry(fullPath,entry,outputFolder,skipExistingFiles);
    }*/

    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //Decrypt list of FSTEntry to Files
    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    public void decryptAllFSTEntriesTo(String outputFolder) throws IOException {
        decryptFSTEntriesTo(true, ".*", outputFolder);
    }
    
    public void decryptFSTEntriesTo(String regEx, String outputFolder) throws IOException {
        decryptFSTEntriesTo(true,regEx, outputFolder);
    }
    public void decryptFSTEntriesTo(boolean fullPath,String regEx, String outputFolder) throws IOException {
       List<FSTEntry> files = getNUSTitle().getAllFSTEntriesFlat();
       Pattern p = Pattern.compile(regEx);
       
       List<FSTEntry> result = new ArrayList<>();
       
       for(FSTEntry f :files){
           String match = f.getFullPath().replaceAll("\\\\", "/");
           Matcher m = p.matcher(match);
           if(m.matches()){
               result.add(f);
           }
       }
       
       decryptFSTEntryListTo(fullPath,result, outputFolder);
    }    
    public void decryptFSTEntryListTo(List<FSTEntry> list,String outputFolder) throws IOException {     
        decryptFSTEntryListTo(true,list,outputFolder);
    }
    public void decryptFSTEntryListTo(boolean fullPath,List<FSTEntry> list,String outputFolder) throws IOException {   
        for(FSTEntry entry: list){
            decryptFSTEntryTo(fullPath,entry, outputFolder, getNUSTitle().isSkipExistingFiles());
        }
    }
    
    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //Save decrypted contents
    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    public void decryptPlainContentByID(int ID,String outputFolder) throws IOException {
        decryptPlainContent(getTMDFromNUSTitle().getContentByID(ID),outputFolder);
    }
    
    public void decryptPlainContentByIndex(int index,String outputFolder) throws IOException {
        decryptPlainContent(getTMDFromNUSTitle().getContentByIndex(index),outputFolder);
    }
    
    public void decryptPlainContent(Content c,String outputFolder) throws IOException {
        decryptPlainContents(new ArrayList<Content>(Arrays.asList(c)), outputFolder);
    }
    
    public void decryptPlainContents(List<Content> list,String outputFolder) throws IOException {
        for(Content c : list){
            decryptContentTo(c,outputFolder,getNUSTitle().isSkipExistingFiles());  
        }
    }    
    public void decryptAllPlainContents(String outputFolder) throws IOException {
        decryptPlainContents(new ArrayList<Content>(getTMDFromNUSTitle().getAllContents().values()),outputFolder);
    }
    
    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //Other
    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    private TMD getTMDFromNUSTitle(){
        return getNUSTitle().getTMD();
    }
}
