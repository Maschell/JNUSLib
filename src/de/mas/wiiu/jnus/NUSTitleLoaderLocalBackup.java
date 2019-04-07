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

import de.mas.wiiu.jnus.entities.Ticket;
import de.mas.wiiu.jnus.implementations.NUSDataProvider;
import de.mas.wiiu.jnus.implementations.NUSDataProviderLocalBackup;

public final class NUSTitleLoaderLocalBackup extends NUSTitleLoader {

    private NUSTitleLoaderLocalBackup() {
        super();
    }

    public static NUSTitle loadNUSTitle(String inputPath) throws Exception {
        return loadNUSTitle(inputPath, (short) Settings.LATEST_TMD_VERSION);
    }

    public static NUSTitle loadNUSTitle(String inputPath, short titleVersion) throws Exception {
        return loadNUSTitle(inputPath, titleVersion, null);
    }

    public static NUSTitle loadNUSTitle(String inputPath, short titleVersion, Ticket ticket) throws Exception {
        NUSTitleLoader loader = new NUSTitleLoaderLocalBackup();
        NUSTitleConfig config = new NUSTitleConfig();

        if (ticket != null) {
            config.setTicket(ticket);
        } else {
            config.setNoDecryption(true);
        }

        config.setVersion(titleVersion);
        config.setInputPath(inputPath);

        return loader.loadNusTitle(config);
    }

    @Override
    protected NUSDataProvider getDataProvider(NUSTitle title, NUSTitleConfig config) {
        return new NUSDataProviderLocalBackup(title, config.getInputPath(), (short) config.getVersion());
    }

}
