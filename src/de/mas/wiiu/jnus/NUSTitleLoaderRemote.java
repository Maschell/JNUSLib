/****************************************************************************
 * Copyright (C) 2016-2018 Maschell
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ****************************************************************************/
package de.mas.wiiu.jnus;

import de.mas.wiiu.jnus.entities.Ticket;
import de.mas.wiiu.jnus.implementations.NUSDataProvider;
import de.mas.wiiu.jnus.implementations.NUSDataProviderRemote;

public final class NUSTitleLoaderRemote extends NUSTitleLoader {

    private NUSTitleLoaderRemote() {
        super();
    }

    public static NUSTitle loadNUSTitle(long titleID) throws Exception {
        return loadNUSTitle(titleID, Settings.LATEST_TMD_VERSION, null);
    }

    public static NUSTitle loadNUSTitle(long titleID, int version) throws Exception {
        return loadNUSTitle(titleID, version, null);
    }

    public static NUSTitle loadNUSTitle(long titleID, Ticket ticket) throws Exception {
        return loadNUSTitle(titleID, Settings.LATEST_TMD_VERSION, ticket);
    }

    public static NUSTitle loadNUSTitle(long titleID, int version, Ticket ticket) throws Exception {
        NUSTitleLoader loader = new NUSTitleLoaderRemote();
        NUSTitleConfig config = new NUSTitleConfig();

        config.setVersion(version);
        config.setTitleID(titleID);
        config.setTicket(ticket);

        return loader.loadNusTitle(config);
    }

    @Override
    protected NUSDataProvider getDataProvider(NUSTitle title, NUSTitleConfig config) {
        return new NUSDataProviderRemote(title, config.getVersion(), config.getTitleID());
    }

}
