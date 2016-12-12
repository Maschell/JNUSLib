package de.mas.jnus.lib.implementations.wud.reader;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;

import de.mas.jnus.lib.implementations.wud.WUDImage;
import de.mas.jnus.lib.implementations.wud.WUDImageCompressedInfo;
import lombok.extern.java.Log;  

@Log
public class WUDDiscReaderCompressed extends WUDDiscReader{
    
    public WUDDiscReaderCompressed(WUDImage image) {
        super(image);
    }
/**
 * Expects the .wux format by Exzap. You can more infos about it here.
 * https://gbatemp.net/threads/wii-u-image-wud-compression-tool.397901/
 */
    @Override
    protected void readEncryptedToOutputStream(OutputStream out, long offset, long size) throws IOException {
        // make sure there is no out-of-bounds read
        WUDImageCompressedInfo info = getImage().getCompressedInfo();
        
        long fileBytesLeft = info.getUncompressedSize() - offset;
        if( fileBytesLeft <= 0 ){
            log.warning("offset too big");
            System.exit(1);
        }
        if( fileBytesLeft < size ){
            size = fileBytesLeft;
        }
        // compressed read must be handled on a per-sector level
        
        int bufferSize = 0x8000;
        byte[] buffer = new byte[bufferSize];
      
        RandomAccessFile input = getRandomAccessFileStream();
        while( size > 0 ){
            long sectorOffset = (offset % info.getSectorSize());
            long remainingSectorBytes = info.getSectorSize() - sectorOffset;
            long sectorIndex = (offset / info.getSectorSize());
            int bytesToRead = (int) ((remainingSectorBytes<size)?remainingSectorBytes:size); // read only up to the end of the current sector
            // look up real sector index
            long realSectorIndex = info.getSectorIndex((int) sectorIndex);
            long offset2 = info.getOffsetSectorArray() + realSectorIndex*info.getSectorSize()+sectorOffset;
            
            input.seek(offset2);
            int read = input.read(buffer);
            if(read < 0) return;
            try{
                out.write(Arrays.copyOfRange(buffer, 0, bytesToRead));
            }catch(IOException e){
                if(e.getMessage().equals("Pipe closed")){
                    break;
                }else{
                    throw e;
                }
            }
            
            size -= bytesToRead;
            offset += bytesToRead;
        }
        input.close();
    }

}
