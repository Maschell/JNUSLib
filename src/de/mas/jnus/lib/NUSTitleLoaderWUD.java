package de.mas.jnus.lib;
import java.io.File;
import java.nio.file.Files;

import de.mas.jnus.lib.implementations.NUSDataProviderWUD;
import de.mas.jnus.lib.implementations.NUSDataProvider;
import de.mas.jnus.lib.implementations.wud.WUDImage;
import de.mas.jnus.lib.implementations.wud.parser.WUDInfo;
import de.mas.jnus.lib.implementations.wud.parser.WUDInfoParser;

public class NUSTitleLoaderWUD extends NUSTitleLoader {
   
    private NUSTitleLoaderWUD(){
        super();
    }
      
    public static NUSTitle loadNUSTitle(String WUDPath) throws Exception{
        return loadNUSTitle(WUDPath, null);
    }
    public static NUSTitle loadNUSTitle(String WUDPath, byte[] titleKey) throws Exception{
        NUSTitleLoader loader = new NUSTitleLoaderWUD();
        NUSTitleConfig config = new NUSTitleConfig();
        
        File wudFile = new File(WUDPath);
        if(!wudFile.exists()){
            System.out.println(WUDPath + " does not exist.");
            System.exit(1);
        }
        
        WUDImage image = new WUDImage(wudFile);
        if(titleKey == null){
            File keyFile = new File(wudFile.getParentFile().getPath() + File.separator + Settings.WUD_KEY_FILENAME);
            if(!keyFile.exists()){
                System.out.println(keyFile.getAbsolutePath() + " does not exist and no title key was provided.");
                return null;
            }
            titleKey = Files.readAllBytes(keyFile.toPath());
        }
        WUDInfo wudInfo = WUDInfoParser.createAndLoad(image.getWUDDiscReader(), titleKey);
        if(wudInfo == null){
            return null;
        }
             
        config.setWUDInfo(wudInfo);
        
        return loader.loadNusTitle(config);
    }
   
    @Override
    protected NUSDataProvider getDataProvider(NUSTitleConfig config) {
        NUSDataProviderWUD result = new NUSDataProviderWUD();
        result.setWUDInfo(config.getWUDInfo());
        return result;
    }

}
