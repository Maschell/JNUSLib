package de.mas.jnus.lib;

import de.mas.jnus.lib.entities.Ticket;
import de.mas.jnus.lib.implementations.NUSDataProviderRemote;
import de.mas.jnus.lib.implementations.NUSDataProvider;

public class NUSTitleLoaderRemote extends NUSTitleLoader{

    private NUSTitleLoaderRemote(){
        super();
    }
    
    public static NUSTitle loadNUSTitle(long titleID) throws Exception{
        return loadNUSTitle(titleID, Settings.LATEST_TMD_VERSION ,null);
    }
    
    public static NUSTitle loadNUSTitle(long titleID, int version) throws Exception{
        return loadNUSTitle(titleID, version,null);
    }
    
    public static NUSTitle loadNUSTitle(long titleID, Ticket ticket) throws Exception{
        return loadNUSTitle(titleID, Settings.LATEST_TMD_VERSION, ticket);
    }
    
    public static NUSTitle loadNUSTitle(long titleID,int version, Ticket ticket) throws Exception{
        NUSTitleLoader loader = new NUSTitleLoaderRemote();
        NUSTitleConfig config = new NUSTitleConfig();

        config.setVersion(version);
        config.setTitleID(titleID);
        
        return loader.loadNusTitle(config);
    }
 
    @Override
    protected NUSDataProvider getDataProvider(NUSTitleConfig config) {
        NUSDataProviderRemote result = new NUSDataProviderRemote();
        result.setVersion(config.getVersion());
        result.setTitleID(config.getTitleID());
        return result;
    }

}
