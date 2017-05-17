package de.mas.wiiu.jnus;

import de.mas.wiiu.jnus.entities.Ticket;
import de.mas.wiiu.jnus.implementations.NUSDataProvider;
import de.mas.wiiu.jnus.implementations.NUSDataProviderLocal;

public final class NUSTitleLoaderLocal extends NUSTitleLoader {

    private NUSTitleLoaderLocal() {
        super();
    }

    public static NUSTitle loadNUSTitle(String inputPath) throws Exception {
        return loadNUSTitle(inputPath, null);
    }

    public static NUSTitle loadNUSTitle(String inputPath, Ticket ticket) throws Exception {
        NUSTitleLoader loader = new NUSTitleLoaderLocal();
        NUSTitleConfig config = new NUSTitleConfig();

        if (ticket != null) {
            config.setTicket(ticket);
        }
        config.setInputPath(inputPath);

        return loader.loadNusTitle(config);
    }

    @Override
    protected NUSDataProvider getDataProvider(NUSTitle title, NUSTitleConfig config) {
        return new NUSDataProviderLocal(title, config.getInputPath());
    }

}
