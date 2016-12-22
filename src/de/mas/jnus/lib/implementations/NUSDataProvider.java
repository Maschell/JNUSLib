package de.mas.jnus.lib.implementations;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.SynchronousQueue;

import com.sun.istack.internal.NotNull;

import de.mas.jnus.lib.NUSTitle;
import de.mas.jnus.lib.Settings;
import de.mas.jnus.lib.entities.content.Content;
import de.mas.jnus.lib.utils.FileUtils;
import de.mas.jnus.lib.utils.Utils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

@Log
/**
 * Service Methods for loading NUS/Content data from
 * different sources
 * @author Maschell
 *
 */
public abstract class NUSDataProvider {
    
    @Getter @Setter private NUSTitle NUSTitle = null;
    
    public NUSDataProvider () {       
    }       
    
    /**
     * Saves the given content encrypted with his .h3 file in the given directory.
     * The Target directory will be created if it's missing.
     * If the content is not hashed, no .h3 will be saved
     * @param content Content that should be saved
     * @param outputFolder Target directory where the files will be stored in.
     * @throws IOException
     */
    public void saveEncryptedContentWithH3Hash(@NotNull Content content,@NotNull  String outputFolder) throws IOException { 
        saveContentH3Hash(content, outputFolder);
        saveEncryptedContent(content, outputFolder);
    }
    
    /**
     * Saves the .h3 file of the given content into the given directory.
     * The Target directory will be created if it's missing.
     * If the content is not hashed, no .h3 will be saved
     * @param content The content of which the h3 hashes should be saved
     * @param outputFolder
     * @throws IOException
     */
    public void saveContentH3Hash(@NotNull Content content,@NotNull String outputFolder) throws IOException { 
        if(!content.isHashed()){
            return;
        }
        byte[] hash = getContentH3Hash(content);
        if(hash == null){
            return;
        }
        String h3Filename = String.format("%08X%s", content.getID(),Settings.H3_EXTENTION);
        File output = new File(outputFolder + File.separator + h3Filename);
        if(output.exists() && output.length() == hash.length){   
            System.out.println(h3Filename + " already exists");
            return;
        }

        System.out.println("Saving " + h3Filename +" ");
       
        FileUtils.saveByteArrayToFile(output, hash);
    }
    
    /**
     * Saves the given content encrypted in the given directory.
     * The Target directory will be created if it's missing.
     * If the content is not encrypted at all, it will be just saved anyway.
      * @param content Content that should be saved
     * @param outputFolder Target directory where the files will be stored in.
     * @throws IOException
     */
    public void saveEncryptedContent(@NotNull Content content,@NotNull String outputFolder) throws IOException {
        Utils.createDir(outputFolder);
        InputStream inputStream = getInputStreamFromContent(content, 0);
        if(inputStream == null){
            log.info("Couldn't save encrypted content. Input stream was null");
            return;
        }
        
        File output = new File(outputFolder + File.separator + content.getFilename());
        if(output.exists()){
            if(output.length() ==  content.getEncryptedFileSize()){        
                log.info("Encrypted content alreadys exists, skipped");
                return;
            }else{
                log.info("Encrypted content alreadys exists, but the length is not as expected. Saving it again");
            }
        }
        System.out.println("Saving " + content.getFilename());
        FileUtils.saveInputStreamToFile(output,inputStream,content.getEncryptedFileSize());
    }

    /**
     * 
     * @param content
     * @param offset
     * @return
     * @throws IOException
     */
    public abstract InputStream getInputStreamFromContent(Content content,long offset)  throws IOException ;

    public abstract byte[] getContentH3Hash(Content content) throws IOException;
    public abstract byte[] getRawTMD() throws IOException;
    public abstract byte[] getRawTicket() throws IOException;
    public abstract byte[] getRawCert() throws IOException;
    
    public abstract void cleanup() throws IOException;
}
