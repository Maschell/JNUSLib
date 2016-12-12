package de.mas.jnus.lib;

import de.mas.jnus.lib.entities.Ticket;
import de.mas.jnus.lib.implementations.NUSDataProviderLocal;
import de.mas.jnus.lib.implementations.NUSDataProvider;

public class NUSTitleLoaderLocal extends NUSTitleLoader {
    
    private NUSTitleLoaderLocal(){
        super();
    }
    public static NUSTitle loadNUSTitle(String inputPath) throws Exception{
        return loadNUSTitle(inputPath, null);
    }
    
    public static NUSTitle loadNUSTitle(String inputPath, Ticket ticket) throws Exception{
        NUSTitleLoader loader = new NUSTitleLoaderLocal();
        NUSTitleConfig config = new NUSTitleConfig();
        
        if(ticket != null){
            config.setTicket(ticket);
        }
        config.setInputPath(inputPath);
        
        return loader.loadNusTitle(config);
    }

    @Override
    protected NUSDataProvider getDataProvider(NUSTitleConfig config) {
        NUSDataProviderLocal result = new NUSDataProviderLocal();
        result.setLocalPath(config.getInputPath());
        return result;
    }

}
