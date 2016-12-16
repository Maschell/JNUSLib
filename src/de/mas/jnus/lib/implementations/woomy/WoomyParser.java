package de.mas.jnus.lib.implementations.woomy;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.sun.istack.internal.NotNull;

import de.mas.jnus.lib.Settings;
import de.mas.jnus.lib.implementations.woomy.WoomyMeta.WoomyEntry;
import lombok.extern.java.Log;

/**
 * 
 * @author Maschell
 *
 */
@Log
public final class WoomyParser {
    private WoomyParser(){
        //
    }
    public static WoomyInfo createWoomyInfo(File woomyFile) throws IOException, ParserConfigurationException, SAXException{
        WoomyInfo result = new WoomyInfo();
        if(!woomyFile.exists()){
            log.info("File does not exist." + woomyFile.getAbsolutePath());
            return null;
        }
        try (ZipFile zipFile = new ZipFile(woomyFile)) {
            result.setWoomyFile(woomyFile);
            ZipEntry metaFile = zipFile.getEntry(Settings.WOOMY_METADATA_FILENAME);
            if(metaFile == null){
                log.info("No meta ");
                return null;
            }
            WoomyMeta meta = WoomyMetaParser.parseMeta(zipFile.getInputStream(metaFile));
            
            /**
             * Currently we will only use the first entry in the metadata.xml
             */
            if(meta.getEntries().isEmpty()){
                return null;
            }
            WoomyEntry entry = meta.getEntries().get(0);
            String regEx = entry.getFolder() + ".*"; //We want all files in the entry fodler
            Map<String,ZipEntry> contentFiles = loadFileList(zipFile,regEx);
            result.setContentFiles(contentFiles);
          
        }catch(ZipException e){
            
        }
        return result;
    }

    private static Map<String, ZipEntry> loadFileList(@NotNull ZipFile zipFile, @NotNull  String regEx) {
        Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
        Map<String,ZipEntry> result = new HashMap<>();
        Pattern pattern = Pattern.compile(regEx);
        while (zipEntries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) zipEntries.nextElement();
            if(!entry.isDirectory()){
                String entryName = entry.getName();
                Matcher matcher = pattern.matcher(entryName);
                if(matcher.matches()){
                    String[] tokens = entryName.split("[\\\\|/]"); //We only want the filename!
                    String filename = tokens[tokens.length - 1];
                    result.put(filename.toLowerCase(Locale.ENGLISH), entry);
                }
            }
        }            
        return result;
    }
}
