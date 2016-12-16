package de.mas.jnus.lib.utils.cryptography;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import de.mas.jnus.lib.entities.Ticket;
import de.mas.jnus.lib.utils.ByteArrayBuffer;
import de.mas.jnus.lib.utils.HashUtil;
import de.mas.jnus.lib.utils.StreamUtils;
import de.mas.jnus.lib.utils.Utils;
import lombok.extern.java.Log;
@Log    
public class NUSDecryption extends AESDecryption{
    public NUSDecryption(byte[] AESKey, byte[] IV) {
        super(AESKey, IV);
    }

    public NUSDecryption(Ticket ticket) {
       this(ticket.getDecryptedKey(),ticket.getIV());
    }

    private byte[] decryptFileChunk(byte[] blockBuffer, int BLOCKSIZE, byte[] IV) {
        return  decryptFileChunk(blockBuffer,0,BLOCKSIZE, IV);
    }
        
    private byte[] decryptFileChunk(byte[] blockBuffer, int offset, int BLOCKSIZE, byte[] IV) {
        if(IV != null){
            setIV(IV);
            init();
        }
        return decrypt(blockBuffer,offset,BLOCKSIZE);
    }
   
    public void decryptFileStream(InputStream inputStream,OutputStream outputStream,long filesize,short contentIndex,byte[] h3hash,long expectedSizeForHash) throws IOException {
        MessageDigest sha1 = null;
        if(h3hash != null){
            try {
                sha1 = MessageDigest.getInstance("SHA1");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        
        int BLOCKSIZE = 0x8000;        
        long dlFileLength = filesize;
        if(dlFileLength > (dlFileLength/BLOCKSIZE)*BLOCKSIZE){
            dlFileLength = ((dlFileLength/BLOCKSIZE)*BLOCKSIZE) +BLOCKSIZE;         
        }
    
        byte[] IV = new byte[0x10];
        
        IV[0] = (byte)((contentIndex >> 8) & 0xFF);
        IV[1] = (byte)(contentIndex);      
             
        byte[] blockBuffer = new byte[BLOCKSIZE];
        
        int inBlockBuffer;      
        long written = 0;
        
        ByteArrayBuffer overflow = new ByteArrayBuffer(BLOCKSIZE);
                    
        do{ 
            inBlockBuffer = StreamUtils.getChunkFromStream(inputStream,blockBuffer,overflow,BLOCKSIZE);
                       
            byte[] output = decryptFileChunk(blockBuffer,BLOCKSIZE,IV);
            IV = Arrays.copyOfRange(blockBuffer,BLOCKSIZE-16, BLOCKSIZE);
             
            if((written + inBlockBuffer) > filesize){                   
                inBlockBuffer = (int) (filesize- written);                  
            }

            written += inBlockBuffer;
           
            outputStream.write(output, 0, inBlockBuffer);
            
            if(sha1 != null){
                sha1.update(output,0,inBlockBuffer);
            }
        }while(inBlockBuffer == BLOCKSIZE);
 
        if(sha1 != null){
            long missingInHash =  expectedSizeForHash - written;   
            if(missingInHash > 0){
                sha1.update(new byte[(int) missingInHash]);
            }
            
            byte[] calculated_hash = sha1.digest();
            byte[] expected_hash = h3hash;
            if(!Arrays.equals(calculated_hash, expected_hash)){
                log.info(Utils.ByteArrayToString(calculated_hash));
                log.info(Utils.ByteArrayToString(expected_hash));
                log.info("Hash doesn't match decrypted content.");
            }else{
                //log.warning("Hash DOES match saves output stream.");
            }
        }
        
        outputStream.close();
        inputStream.close();
    }
    
    public void decryptFileStreamHashed(InputStream inputStream,OutputStream outputStream,long filesize,long fileoffset,short contentIndex,byte[] h3Hash) throws IOException{
        int BLOCKSIZE = 0x10000;
        int HASHBLOCKSIZE = 0xFC00;
        
        long writeSize = HASHBLOCKSIZE;     
        
        long block = (fileoffset / HASHBLOCKSIZE);         
        long soffset = fileoffset - (fileoffset / HASHBLOCKSIZE * HASHBLOCKSIZE);   
        
        if( soffset+filesize > writeSize )
            writeSize = writeSize - soffset;
                
        byte[] encryptedBlockBuffer = new byte[BLOCKSIZE];
        ByteArrayBuffer overflow = new ByteArrayBuffer(BLOCKSIZE);
    
        long wrote = 0;
        int inBlockBuffer;
        do{
            inBlockBuffer = StreamUtils.getChunkFromStream(inputStream,encryptedBlockBuffer,overflow,BLOCKSIZE);
            
            if( writeSize > filesize )
                writeSize = filesize;
            
            byte[] output = decryptFileChunkHash(encryptedBlockBuffer, (int) block,contentIndex,h3Hash);
            
            if((wrote + writeSize) > filesize){                   
                writeSize = (int) (filesize- wrote);
            }
            
            outputStream.write(output, (int)(0+soffset), (int)writeSize);
            
            wrote +=writeSize;
            
            block++;
            
            if( soffset > 0){
                writeSize = HASHBLOCKSIZE;
                soffset = 0;
            }
        }while(wrote < filesize && (inBlockBuffer == BLOCKSIZE));
        
        outputStream.close();
        inputStream.close();
    }
    
    private byte[] decryptFileChunkHash(byte[] blockBuffer, int block, int contentIndex,byte[] h3_hashes){
        int hashSize = 0x400;
        int blocksize = 0xFC00;
        byte[] IV = ByteBuffer.allocate(16).putShort((short) contentIndex).array();
            
        byte[] hashes = decryptFileChunk(blockBuffer,hashSize,IV);
        
        hashes[0] ^= (byte)((contentIndex >> 8) & 0xFF);
        hashes[1] ^= (byte)(contentIndex & 0xFF);
                
        int H0_start = (block % 16) * 20;
        
        IV = Arrays.copyOfRange(hashes,H0_start,H0_start + 16);         
        byte[] output =  decryptFileChunk(blockBuffer,hashSize,blocksize,IV);
        
        HashUtil.checkFileChunkHashes(hashes,h3_hashes,output,block);
        
        return output;
    }
}
