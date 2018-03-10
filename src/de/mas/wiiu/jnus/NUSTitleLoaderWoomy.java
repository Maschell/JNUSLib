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

import java.io.File;

import de.mas.wiiu.jnus.implementations.NUSDataProvider;
import de.mas.wiiu.jnus.implementations.NUSDataProviderWoomy;
import de.mas.wiiu.jnus.implementations.woomy.WoomyInfo;
import de.mas.wiiu.jnus.implementations.woomy.WoomyParser;
import lombok.extern.java.Log;

@Log
public final class NUSTitleLoaderWoomy extends NUSTitleLoader {

    public static NUSTitle loadNUSTitle(String inputFile) throws Exception {
        NUSTitleLoaderWoomy loader = new NUSTitleLoaderWoomy();
        NUSTitleConfig config = new NUSTitleConfig();

        WoomyInfo woomyInfo = WoomyParser.createWoomyInfo(new File(inputFile));
        if (woomyInfo == null) {
            log.info("Created woomy is null.");
            return null;
        }
        config.setWoomyInfo(woomyInfo);
        return loader.loadNusTitle(config);
    }

    @Override
    protected NUSDataProvider getDataProvider(NUSTitle title, NUSTitleConfig config) {
        return new NUSDataProviderWoomy(title, config.getWoomyInfo());
    }

}
