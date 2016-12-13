package de.mas.jnus.lib.implementations.wud.parser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import de.mas.jnus.lib.Settings;
import de.mas.jnus.lib.entities.content.ContentFSTInfo;
import de.mas.jnus.lib.entities.fst.FST;
import de.mas.jnus.lib.entities.fst.FSTEntry;
import de.mas.jnus.lib.implementations.wud.reader.WUDDiscReader;
import de.mas.jnus.lib.utils.ByteUtils;
import de.mas.jnus.lib.utils.FileUtils;
import de.mas.jnus.lib.utils.Utils;
import lombok.extern.java.Log;

@Log
public class WUDInfoParser {
    public static byte[] DECRYPTED_AREA_SIGNATURE = new byte[] { (byte) 0xCC, (byte) 0xA6, (byte) 0xE6, 0x7B };
    public static byte[] PARTITION_FILE_TABLE_SIGNATURE = new byte[] { 0x46, 0x53, 0x54, 0x00 }; // "FST"
    public final static int PARTITION_TOC_OFFSET = 0x800;
    public final static int PARTITION_TOC_ENTRY_SIZE = 0x80;
    
    public static final String WUD_TMD_FILENAME = "title.tmd";
    public static final String WUD_TICKET_FILENAME = "title.tik";
    public static final String WUD_CERT_FILENAME = "title.cert";
    
    public static WUDInfo createAndLoad(WUDDiscReader discReader,byte[] titleKey) throws IOException {
        WUDInfo result = new WUDInfo();
        
        result.setTitleKey(titleKey);
        result.setWUDDiscReader(discReader);
        
        byte[] PartitionTocBlock = discReader.readDecryptedToByteArray(Settings.WIIU_DECRYPTED_AREA_OFFSET, 0, 0x8000, titleKey, null);

        // verify DiscKey before proceeding
        if(!Arrays.equals(Arrays.copyOfRange(PartitionTocBlock, 0, 4),DECRYPTED_AREA_SIGNATURE)){
            System.out.println("Decryption of PartitionTocBlock failed");
            return null;
        }
        
        Map<String,WUDPartition> partitions = readPartitions(result,PartitionTocBlock);
        result.setPartitions(partitions);
        //parsePartitions(wudInfo,partitions);
        
        return result;
    }

    
    private static Map<String, WUDPartition> readPartitions(WUDInfo wudInfo,byte[] partitionTocBlock) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(partitionTocBlock.length);
        
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.put(partitionTocBlock);
        buffer.position(0);
                
        int partitionCount = (int) ByteUtils.getUnsingedIntFromBytes(partitionTocBlock, 0x1C,ByteOrder.BIG_ENDIAN);

        Map<String,WUDPartition> partitions = new HashMap<>();
        
        WUDGamePartition gamePartition = new WUDGamePartition();
    
        String realGamePartitionName = null;
        // populate partition information from decrypted TOC
        for (int i = 0; i < partitionCount; i++){
            WUDPartition partition = new WUDPartition();

            int offset = (PARTITION_TOC_OFFSET + (i * PARTITION_TOC_ENTRY_SIZE));
            byte[] partitionIdentifier = Arrays.copyOfRange(partitionTocBlock, offset, offset+ 0x19);
            int j = 0;
            for(j = 0;j<partitionIdentifier.length;j++){
                if(partitionIdentifier[j] == 0){
                    break;
                }
            }
            String partitionName = new String(Arrays.copyOfRange(partitionIdentifier,0,j));

            // calculate partition offset (relative from WIIU_DECRYPTED_AREA_OFFSET) from decrypted TOC
            long tmp = ByteUtils.getUnsingedIntFromBytes(partitionTocBlock, (PARTITION_TOC_OFFSET + (i * PARTITION_TOC_ENTRY_SIZE) + 0x20), ByteOrder.BIG_ENDIAN);
           
            long partitionOffset = ((tmp * (long)0x8000) - 0x10000);
            
            
            partition.setPartitionName(partitionName);  
            partition.setPartitionOffset(partitionOffset);
            
            if(partitionName.startsWith("SI")){
                byte[] fileTableBlock = wudInfo.getWUDDiscReader().readDecryptedToByteArray(Settings.WIIU_DECRYPTED_AREA_OFFSET + partitionOffset,0, 0x8000, wudInfo.getTitleKey(),null);
                if(!Arrays.equals(Arrays.copyOfRange(fileTableBlock, 0, 4),PARTITION_FILE_TABLE_SIGNATURE)){
                    log.info("FST Decrpytion failed");
                    continue;
                }
         
                FST fst = FST.parseFST(fileTableBlock, null);
                
                byte[] rawTIK = getFSTEntryAsByte(WUD_TICKET_FILENAME,partition,fst,wudInfo.getWUDDiscReader(),wudInfo.getTitleKey());
                byte[] rawTMD = getFSTEntryAsByte(WUD_TMD_FILENAME,partition,fst,wudInfo.getWUDDiscReader(),wudInfo.getTitleKey());
                byte[] rawCert = getFSTEntryAsByte(WUD_CERT_FILENAME,partition,fst,wudInfo.getWUDDiscReader(),wudInfo.getTitleKey());
                
                gamePartition.setRawTMD(rawTMD);
                gamePartition.setRawTicket(rawTIK);
                gamePartition.setRawCert(rawCert);
                
                //We want to use the real game partition
                realGamePartitionName = partitionName = "GM" + Utils.ByteArrayToString(Arrays.copyOfRange(rawTIK, 0x1DC, 0x1DC + 0x08));
            }else if(partitionName.startsWith(realGamePartitionName)){                
                gamePartition.setPartitionOffset(partitionOffset);
                gamePartition.setPartitionName(partitionName);
                
                wudInfo.setGamePartitionName(partitionName);
                partition = gamePartition;
            }
            byte [] header = wudInfo.getWUDDiscReader().readEncryptedToByteArray(partition.getPartitionOffset()+0x10000,0,0x8000);
            WUDPartitionHeader partitionHeader = WUDPartitionHeader.parseHeader(header);
            partition.setPartitionHeader(partitionHeader);
            
            partitions.put(partitionName, partition);
        }
       
        return partitions;
    }
 
    private static byte[] getFSTEntryAsByte(String filename, WUDPartition partition,FST fst,WUDDiscReader discReader,byte[] key) throws IOException{
        FSTEntry entry = getEntryByName(fst.getRoot(),filename);
        ContentFSTInfo info = fst.getContentFSTInfos().get((int)entry.getContentFSTID());
        
        //Calculating the IV
        ByteBuffer byteBuffer = ByteBuffer.allocate(0x10);
        byteBuffer.position(0x08);
        byte[] IV = byteBuffer.putLong(entry.getFileOffset()>>16).array();    
        
        return discReader.readDecryptedToByteArray(Settings.WIIU_DECRYPTED_AREA_OFFSET + (long)partition.getPartitionOffset() + (long)info.getOffset(),entry.getFileOffset(), (int) entry.getFileSize(), key, IV);
    }
        
    private static FSTEntry getEntryByName(FSTEntry root,String name){
        for(FSTEntry cur : root.getFileChildren()){
            if(cur.getFilename().equals(name)){
                return cur;
            }
        }
        for(FSTEntry cur : root.getDirChildren()){
            FSTEntry dir_result = getEntryByName(cur,name);
            if(dir_result != null){
                return dir_result;
            }
        }
        return null;
    }
}
