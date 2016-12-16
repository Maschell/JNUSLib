package de.mas.jnus.lib;
import java.io.File;
import java.nio.file.Files;

import de.mas.jnus.lib.implementations.NUSDataProviderWUD;
import de.mas.jnus.lib.implementations.NUSDataProvider;
import de.mas.jnus.lib.implementations.wud.WUDImage;
import de.mas.jnus.lib.implementations.wud.parser.WUDInfo;
import de.mas.jnus.lib.implementations.wud.parser.WUDInfoParser;

public final class NUSTitleLoaderWUD extends NUSTitleLoader {
   
    private NUSTitleLoaderWUD(){
        super();
    }
      
    public static NUSTitle loadNUSTitle(String WUDPath) throws Exception{
        return loadNUSTitle(WUDPath, null);
    }
    public static NUSTitle loadNUSTitle(String WUDPath, byte[] titleKey) throws Exception{
        NUSTitleLoader loader = new NUSTitleLoaderWUD();
        NUSTitleConfig config = new NUSTitleConfig();
        byte[] usedTitleKey = titleKey;
        File wudFile = new File(WUDPath);
        if(!wudFile.exists()){
            System.out.println(WUDPath + " does not exist.");
            System.exit(1);
        }
        
        WUDImage image = new WUDImage(wudFile);
        if(usedTitleKey == null){
            File keyFile = new File(wudFile.getParentFile().getPath() + File.separator + Settings.WUD_KEY_FILENAME);
            if(!keyFile.exists()){
                System.out.println(keyFile.getAbsolutePath() + " does not exist and no title key was provided.");
                return null;
            }
            usedTitleKey = Files.readAllBytes(keyFile.toPath());
        }
        WUDInfo wudInfo = WUDInfoParser.createAndLoad(image.getWUDDiscReader(), usedTitleKey);
        if(wudInfo == null){
            return null;
        }
             
        config.setWUDInfo(wudInfo);
        
        return loader.loadNusTitle(config);
    }
   
    @Override
    protected NUSDataProvider getDataProvider(NUSTitle title,NUSTitleConfig config) {
        return new NUSDataProviderWUD(title,config.getWUDInfo());
    }

}
