/****************************************************************************
 * Copyright (C) 2016-2019 Maschell
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

import java.io.IOException;
import java.text.ParseException;

import de.mas.wiiu.jnus.entities.Ticket;
import de.mas.wiiu.jnus.implementations.DefaultNUSDataProcessor;
import de.mas.wiiu.jnus.implementations.NUSDataProviderRemote;
import de.mas.wiiu.jnus.utils.download.NUSDownloadService;

public final class NUSTitleLoaderRemote {

    private NUSTitleLoaderRemote() {
    }

    public static NUSTitle loadNUSTitle(long titleID, byte[] commonKey) throws Exception {
        return loadNUSTitle(titleID, Settings.LATEST_TMD_VERSION, commonKey);
    }

    public static NUSTitle loadNUSTitle(long titleID, int version, byte[] commonKey) throws Exception {
        return loadNUSTitle(titleID, version, null, false, commonKey);
    }

    public static NUSTitle loadNUSTitle(long titleID, Ticket ticket) throws Exception {
        return loadNUSTitle(titleID, Settings.LATEST_TMD_VERSION, ticket);
    }

    public static NUSTitle loadNUSTitle(long titleID, int version, Ticket ticket) throws Exception {
        return loadNUSTitle(titleID, version, ticket, false, null);
    }

    public static NUSTitle loadNUSTitle(long titleID, int version, Ticket ticket, boolean noEncryption, byte[] commonKey) throws IOException, ParseException {
        NUSTitleConfig config = new NUSTitleConfig();

        config.setTicket(ticket);
        config.setNoDecryption(noEncryption);
        config.setCommonKey(commonKey);
        if (ticket == null && !noEncryption && commonKey == null) {
            throw new IOException("Ticket was null and no commonKey was given");
        }

        return NUSTitleLoader.loadNusTitle(config, () -> new NUSDataProviderRemote(version, titleID, NUSDownloadService.getDefaultInstance()), (dp, cd, en) -> new DefaultNUSDataProcessor(dp, cd));
    }

}
