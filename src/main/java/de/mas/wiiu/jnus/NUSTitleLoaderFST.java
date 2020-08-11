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

import de.mas.wiiu.jnus.entities.FST.nodeentry.DirectoryEntry;
import de.mas.wiiu.jnus.implementations.DefaultNUSDataProcessor;
import de.mas.wiiu.jnus.implementations.NUSDataProviderFST;
import de.mas.wiiu.jnus.interfaces.FSTDataProvider;

public final class NUSTitleLoaderFST {

    private NUSTitleLoaderFST() {
    }

    public static NUSTitle loadNUSTitle(FSTDataProvider dataProvider, byte[] commonKey) throws IOException, ParseException {
        return loadNUSTitle(dataProvider, dataProvider.getRoot(), commonKey);
    }

    public static NUSTitle loadNUSTitle(FSTDataProvider dataProvider, DirectoryEntry base, byte[] commonKey) throws IOException, ParseException {
        NUSTitleConfig config = new NUSTitleConfig();
        config.setCommonKey(commonKey);

        return NUSTitleLoader.loadNusTitle(config, () -> new NUSDataProviderFST(dataProvider, base), (dp, cd, en) -> new DefaultNUSDataProcessor(dp, cd));
    }

}
