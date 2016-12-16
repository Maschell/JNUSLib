package de.mas.jnus.lib;

import java.io.File;

import de.mas.jnus.lib.implementations.NUSDataProviderWoomy;
import de.mas.jnus.lib.implementations.NUSDataProvider;
import de.mas.jnus.lib.implementations.woomy.WoomyInfo;
import de.mas.jnus.lib.implementations.woomy.WoomyParser;
import lombok.extern.java.Log;

@Log
public final class NUSTitleLoaderWoomy extends NUSTitleLoader {

    public static NUSTitle loadNUSTitle(String inputFile) throws Exception{
        NUSTitleLoaderWoomy loader = new NUSTitleLoaderWoomy();
        NUSTitleConfig config = new NUSTitleConfig();
        
        WoomyInfo woomyInfo = WoomyParser.createWoomyInfo(new File(inputFile));
        if(woomyInfo == null){
            log.info("Created woomy is null.");
            return null;
        }
        config.setWoomyInfo(woomyInfo);
        return loader.loadNusTitle(config);
    }
    @Override
    protected NUSDataProvider getDataProvider(NUSTitle title,NUSTitleConfig config) {
        return new NUSDataProviderWoomy(title,config.getWoomyInfo());
    }

}
