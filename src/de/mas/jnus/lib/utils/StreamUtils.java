package de.mas.jnus.lib.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import lombok.extern.java.Log;

@Log
public class StreamUtils {
    public static byte[] getBytesFromStream(InputStream in,int size) throws IOException{
        byte[] result = new byte[size];
        byte[] buffer = new byte[0x8000];
        int totalRead = 0;
        do{ 
            int read = in.read(buffer);
            if(read < 0) break;
            System.arraycopy(buffer, 0, result, totalRead, read);
            totalRead += read;
        }while(totalRead < size);
        in.close();
        return result;
    }
    
    public static int getChunkFromStream(InputStream inputStream,byte[] output, ByteArrayBuffer overflowbuffer,int BLOCKSIZE) throws IOException {
        int bytesRead = -1;
        int inBlockBuffer = 0;
        byte[] overflowbuf = overflowbuffer.getBuffer();
        do{
            try{
            bytesRead = inputStream.read(overflowbuf,overflowbuffer.getLengthOfDataInBuffer(),overflowbuffer.getSpaceLeft());
            }catch(IOException e){
                if(!e.getMessage().equals("Write end dead")){
                    throw e;
                }
                bytesRead = -1;
            }
            if(bytesRead <= 0){
                if(overflowbuffer.getLengthOfDataInBuffer() > 0){
                    System.arraycopy(overflowbuf, 0, output, 0, overflowbuffer.getLengthOfDataInBuffer());
                    inBlockBuffer = overflowbuffer.getLengthOfDataInBuffer();
                }
                break;
            }
            
            overflowbuffer.addLengthOfDataInBuffer(bytesRead);
            
            if(inBlockBuffer + overflowbuffer.getLengthOfDataInBuffer() > BLOCKSIZE){
                int tooMuch = (inBlockBuffer + bytesRead) - BLOCKSIZE;
                int toRead = BLOCKSIZE - inBlockBuffer;
                
                System.arraycopy(overflowbuf, 0, output, inBlockBuffer, toRead);
                inBlockBuffer += toRead;
                
                System.arraycopy(overflowbuf, toRead, overflowbuf, 0, tooMuch);
                overflowbuffer.setLengthOfDataInBuffer(tooMuch);
            }else{     
                System.arraycopy(overflowbuf, 0, output, inBlockBuffer, overflowbuffer.getLengthOfDataInBuffer()); 
                inBlockBuffer +=overflowbuffer.getLengthOfDataInBuffer();
                overflowbuffer.resetLengthOfDataInBuffer();
            }
        }while(inBlockBuffer != BLOCKSIZE);
        return inBlockBuffer;
    }
    
    public static void saveInputStreamToOutputStream(InputStream inputStream, OutputStream outputStream, long filesize) throws IOException {
        saveInputStreamToOutputStreamWithHash(inputStream, outputStream, filesize, null,0L);
    }

    public static void saveInputStreamToOutputStreamWithHash(InputStream inputStream, OutputStream outputStream,long filesize, byte[] hash,long expectedSizeForHash) throws IOException {
        MessageDigest sha1 = null;       
        if(hash != null){
            try {
                sha1 = MessageDigest.getInstance("SHA1");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        
        int BUFFER_SIZE = 0x8000;
        byte[] buffer = new byte[BUFFER_SIZE];
        int read = 0;
        long totalRead = 0;
        long written = 0;
        do{
            read = inputStream.read(buffer);
            if(read < 0){
                break;
            }
            totalRead +=read;
            
            if(totalRead > filesize){
                read = (int) (read - (totalRead - filesize));
            }
            
            outputStream.write(buffer, 0, read);
            written += read;
            
            if(sha1 != null){
                sha1.update(buffer,0,read);
            }
        }while(written < filesize);
        
        if(sha1 != null){
            long missingInHash =  expectedSizeForHash - written;   
            if(missingInHash > 0){
                sha1.update(new byte[(int) missingInHash]);
            }
            
            byte[] calculated_hash = sha1.digest();
            byte[] expected_hash = hash;
            if(!Arrays.equals(calculated_hash, expected_hash)){
                log.info(Utils.ByteArrayToString(calculated_hash));
                log.info(Utils.ByteArrayToString(expected_hash));
                log.info("Hash doesn't match saves output stream.");
            }else{
                //log.warning("Hash DOES match saves output stream.");
            }
        }
        
        outputStream.close();        
        inputStream.close();
    }
}
