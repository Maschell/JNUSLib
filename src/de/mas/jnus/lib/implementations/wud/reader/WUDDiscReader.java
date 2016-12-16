package de.mas.jnus.lib.implementations.wud.reader;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;

import de.mas.jnus.lib.implementations.wud.WUDImage;
import de.mas.jnus.lib.utils.cryptography.AESDecryption;
import lombok.Getter;
import lombok.extern.java.Log;
@Log
public abstract class WUDDiscReader {
    @Getter private final WUDImage image;
    
    public WUDDiscReader(WUDImage image){
        this.image = image;
    }
    
    public InputStream readEncryptedToInputStream(long offset,long size) throws IOException {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = new PipedOutputStream(in);

        new Thread(() -> {try {
            readEncryptedToOutputStream(out,offset,size);
        } catch (IOException e) {e.printStackTrace();}}).start();

        return in;
    }
    
    public byte[] readEncryptedToByteArray(long offset,long fileoffset, long size) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        readEncryptedToOutputStream(out,offset,size);        
        return out.toByteArray();
    }
    
    public InputStream readDecryptedToInputStream(long offset,long fileoffset, long size, byte[] key,byte[] iv) throws IOException {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = new PipedOutputStream(in);
                
        new Thread(() -> {try {
            readDecryptedToOutputStream(out,offset,fileoffset,size,key,iv);
        } catch (IOException e) {e.printStackTrace();}}).start();
      
        return in;
    }
    
    public byte[] readDecryptedToByteArray(long offset,long fileoffset, long size, byte[] key,byte[] iv) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
       
        readDecryptedToOutputStream(out,offset,fileoffset,size,key,iv);        
        return out.toByteArray();
    }

    protected abstract void readEncryptedToOutputStream(OutputStream out, long offset, long size) throws IOException;
    
    /**
     * 
     * @param readOffset Needs to be aligned to 0x8000
     * @param key
     * @param IV
     * @return
     * @throws IOException
     */
    public byte[] readDecryptedChunk(long readOffset,byte[] key, byte[]IV) throws IOException{
        int chunkSize = 0x8000;
        
        byte[] encryptedChunk = readEncryptedToByteArray(readOffset, 0, chunkSize);
        byte[] decryptedChunk = new byte[chunkSize];
        
        AESDecryption aesDecryption = new AESDecryption(key, IV);
        decryptedChunk = aesDecryption.decrypt(encryptedChunk);
        
        return decryptedChunk;
    }
    
    public void readDecryptedToOutputStream(OutputStream outputStream,long clusterOffset, long fileOffset, long size,byte[] key,byte[] IV) throws IOException {
        byte[] usedIV = IV;
        if(usedIV == null){
            usedIV = new byte[0x10];
        }
        long usedSize = size;
        long usedFileOffset = fileOffset;
        byte[] buffer;

        long maxCopySize;
        long copySize;

        long readOffset;
        
        int blockSize = 0x8000;
        long totalread = 0;
        
        do{
            long blockNumber = (usedFileOffset / blockSize);
            long blockOffset = (usedFileOffset % blockSize);
            
            readOffset = clusterOffset + (blockNumber * blockSize);
            // (long)WiiUDisc.WIIU_DECRYPTED_AREA_OFFSET + volumeOffset + clusterOffset + (blockStructure.getBlockNumber() * 0x8000);
           
            buffer = readDecryptedChunk(readOffset,key, usedIV);
            maxCopySize = 0x8000 - blockOffset;
            copySize = (usedSize > maxCopySize) ? maxCopySize : usedSize;
                   
            outputStream.write(Arrays.copyOfRange(buffer, (int) blockOffset, (int) copySize));
            totalread += copySize;

            // update counters
            usedSize -= copySize;
            usedFileOffset += copySize;
        }while(totalread < usedSize);
        
        outputStream.close();
    }
   
    public RandomAccessFile getRandomAccessFileStream() throws FileNotFoundException{
        if(getImage() == null ||  getImage().getFileHandle() == null){
            log.warning("No image or image filehandle set.");
            System.exit(1);
        }
        return new RandomAccessFile(getImage().getFileHandle(), "r");
    }
}
