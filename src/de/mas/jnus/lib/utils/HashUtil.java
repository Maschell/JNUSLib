package de.mas.jnus.lib.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import lombok.extern.java.Log;
@Log
public class HashUtil {
    public static byte[] hashSHA256(byte[] data){
        MessageDigest sha256;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");            
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return new byte[0x20]; 
        }       
       
        return sha256.digest(data);
    }
    
    public static byte[] hashSHA256(File file) {
        return hashSHA256(file, 0);
    }
    
    //TODO: testing
    public static byte[] hashSHA256(File file,int aligmnent) {
        byte[] hash = new byte[0x20];
        MessageDigest sha1 = null;
        try {
            InputStream in = new FileInputStream(file);
            sha1 = MessageDigest.getInstance("SHA-256");  
            hash = hash(sha1,in,file.length(),0x8000,aligmnent);
        } catch (NoSuchAlgorithmException | FileNotFoundException e) {            
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }  
       
        return hash;
    }
    
    public static byte[] hashSHA1(byte[] data){
        MessageDigest sha1;
        try {
            sha1 = MessageDigest.getInstance("SHA1");            
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return new byte[0x14]; 
        }       
       
        return sha1.digest(data);
    }
    
    public static byte[] hashSHA1(File file) {
        return hashSHA1(file, 0);
    }
    
    public static byte[] hashSHA1(File file,int aligmnent) {
        byte[] hash = new byte[0x14];
        MessageDigest sha1 = null;
        try {
            InputStream in = new FileInputStream(file);
            sha1 = MessageDigest.getInstance("SHA1");  
            hash = hash(sha1,in,file.length(),0x8000,aligmnent);
        } catch (NoSuchAlgorithmException | FileNotFoundException e) {            
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }  
       
        return hash;
    }
    
    public static byte [] hash(MessageDigest digest, InputStream in,long inputSize1, int bufferSize,int alignment) throws IOException {
        long target_size = alignment == 0 ? inputSize1: Utils.align(inputSize1, alignment);
        long cur_position = 0;
        int inBlockBufferRead = 0;
        byte[] blockBuffer = new byte[bufferSize];
        ByteArrayBuffer overflow = new ByteArrayBuffer(bufferSize);
        do{
            if(cur_position + bufferSize > target_size){               
                int expectedSize = (int) (target_size - cur_position);
                ByteBuffer buffer = ByteBuffer.allocate(expectedSize);
                buffer.position(0);
                inBlockBufferRead = StreamUtils.getChunkFromStream(in,blockBuffer,overflow,expectedSize);
                buffer.put(Arrays.copyOfRange(blockBuffer, 0, inBlockBufferRead));
                blockBuffer = buffer.array();
                inBlockBufferRead = blockBuffer.length;
            }else{
                int expectedSize = bufferSize;
                inBlockBufferRead = StreamUtils.getChunkFromStream(in,blockBuffer,overflow,expectedSize);
            }
            if(inBlockBufferRead == 0){
                inBlockBufferRead = (int) (target_size - cur_position);
                blockBuffer = new byte[inBlockBufferRead];
            }
            if(inBlockBufferRead <= 0) break;

            digest.update(blockBuffer, 0, inBlockBufferRead);
            cur_position += inBlockBufferRead;
            
        }while(cur_position < target_size);
       
        in.close();

        return digest.digest();
    }

    public static void checkFileChunkHashes(byte[] hashes,byte[] h3Hashes, byte[] output,int block) {
        int H0_start = (block % 16) * 20;
        int H1_start = (16 + (block / 16) % 16) * 20;
        int H2_start = (32 + (block / 256) % 16) * 20;
        int H3_start = ((block / 4096) % 16) * 20;
        
        byte[] real_h0_hash = HashUtil.hashSHA1(output);
        byte[] expected_h0_hash = Arrays.copyOfRange(hashes,H0_start,H0_start + 20);

        if(!Arrays.equals(real_h0_hash,expected_h0_hash)){          
            System.out.println("h0 checksum failed");
            System.out.println("real hash    :" + Utils.ByteArrayToString(real_h0_hash));
            System.out.println("expected hash:" + Utils.ByteArrayToString(expected_h0_hash));
            System.exit(2);
            //throw new IllegalArgumentException("h0 checksumfail");            
        }else{
            log.finest("h1 checksum right!");
        }
        
        if ((block % 16) == 0){
            byte[] expected_h1_hash = Arrays.copyOfRange(hashes,H1_start,H1_start + 20);
            byte[] real_h1_hash = HashUtil.hashSHA1(Arrays.copyOfRange(hashes,H0_start,H0_start + (16*20)));
            
            if(!Arrays.equals(expected_h1_hash, real_h1_hash)){            
                System.out.println("h1 checksum failed");
                System.out.println("real hash    :" + Utils.ByteArrayToString(real_h1_hash));
                System.out.println("expected hash:" + Utils.ByteArrayToString(expected_h1_hash));
                System.exit(2);
                //throw new IllegalArgumentException("h1 checksumfail");         
            }else{
                log.finer("h1 checksum right!");
            }
        }
        
        if ((block % 256) == 0){
            byte[] expected_h2_hash = Arrays.copyOfRange(hashes,H2_start,H2_start + 20);
            byte[] real_h2_hash = HashUtil.hashSHA1(Arrays.copyOfRange(hashes,H1_start,H1_start + (16*20)));
           
            if(!Arrays.equals(expected_h2_hash, real_h2_hash)){            
                System.out.println("h2 checksum failed");
                System.out.println("real hash    :" + Utils.ByteArrayToString(real_h2_hash));
                System.out.println("expected hash:" + Utils.ByteArrayToString(expected_h2_hash));
                System.exit(2);
                //throw new IllegalArgumentException("h2 checksumfail");
                
            }else{
                log.fine("h2 checksum right!");
            }
        }
        
        if(h3Hashes == null){
            log.info("didn't check the h3, its missing.");
            return;
        }
        if ((block % 4096) == 0){
            byte[] expected_h3_hash = Arrays.copyOfRange(h3Hashes,H3_start,H3_start + 20);
            byte[] real_h3_hash = HashUtil.hashSHA1(Arrays.copyOfRange(hashes,H2_start,H2_start + (16*20)));

            if(!Arrays.equals(expected_h3_hash, real_h3_hash)){            
                System.out.println("h3 checksum failed");
                System.out.println("real hash    :" + Utils.ByteArrayToString(real_h3_hash));
                System.out.println("expected hash:" + Utils.ByteArrayToString(expected_h3_hash));
                System.exit(2);
                //throw new IllegalArgumentException("h3 checksumfail");         
            }else{
                log.fine("h3 checksum right!");
            }
        }        
    }
}
