package de.mas.jnus.lib.implementations;

import java.io.IOException;
import java.io.InputStream;

import de.mas.jnus.lib.NUSTitle;
import de.mas.jnus.lib.Settings;
import de.mas.jnus.lib.entities.TMD;
import de.mas.jnus.lib.entities.content.Content;
import de.mas.jnus.lib.implementations.wud.parser.WUDInfo;
import de.mas.jnus.lib.implementations.wud.parser.WUDPartition;
import de.mas.jnus.lib.implementations.wud.parser.WUDPartitionHeader;
import de.mas.jnus.lib.implementations.wud.reader.WUDDiscReader;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
@Log
public class NUSDataProviderWUD extends NUSDataProvider {
    @Getter private final WUDInfo WUDInfo;
    
    @Setter(AccessLevel.PRIVATE) private TMD TMD = null;
    
    public NUSDataProviderWUD(NUSTitle title,WUDInfo wudinfo) {
        super(title);
        this.WUDInfo = wudinfo;
    }

    public long getOffsetInWUD(Content content) {
        if(content.getContentFSTInfo() == null){
            return getAbsoluteReadOffset();
        }else{
            return getAbsoluteReadOffset() + content.getContentFSTInfo().getOffset();            
        }
    }
    
    public long getAbsoluteReadOffset(){
        return (long)Settings.WIIU_DECRYPTED_AREA_OFFSET + getGamePartition().getPartitionOffset();
    }
    
    @Override
    public InputStream getInputStreamFromContent(Content content, long fileOffsetBlock) throws IOException {
        WUDDiscReader discReader = getDiscReader();            
        long offset = getOffsetInWUD(content) + fileOffsetBlock;
        return discReader.readEncryptedToInputStream(offset, content.getEncryptedFileSize());
    }

    @Override
    public byte[] getContentH3Hash(Content content) throws IOException {

        if(getGamePartitionHeader() == null){
            log.warning("GamePartitionHeader is null");
            return null;
        }
        
        if(!getGamePartitionHeader().isCalculatedHashes()){
            log.info("Calculating h3 hashes");
            getGamePartitionHeader().calculateHashes(getTMD().getAllContents());
            
        }
        return getGamePartitionHeader().getH3Hash(content);
        
    }
    
    private TMD getTMD() {
        if(TMD == null){
            setTMD(de.mas.jnus.lib.entities.TMD.parseTMD(getRawTMD()));
        }
        return TMD;
    }

    @Override
    public byte[] getRawTMD() {
        return getWUDInfo().getGamePartition().getRawTMD();
    }

    @Override
    public byte[] getRawTicket() {
        return getWUDInfo().getGamePartition().getRawTicket();
    }
    
    @Override
    public byte[] getRawCert() throws IOException {
        return getWUDInfo().getGamePartition().getRawCert();
    }
    
    
    public WUDPartition getGamePartition(){
        return getWUDInfo().getGamePartition();
    }
    
    public WUDPartitionHeader getGamePartitionHeader(){
        return getGamePartition().getPartitionHeader();
    }
    
    public WUDDiscReader getDiscReader(){
        return getWUDInfo().getWUDDiscReader();
    }

    @Override
    public void cleanup() {
        //We don't need it
    }
  
}
