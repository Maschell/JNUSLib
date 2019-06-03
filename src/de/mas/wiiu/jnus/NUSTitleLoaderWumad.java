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

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import de.mas.wiiu.jnus.implementations.wumad.WumadInfo;
import de.mas.wiiu.jnus.implementations.wumad.WumadParser;

public final class NUSTitleLoaderWumad {

    private NUSTitleLoaderWumad() {

    }

    public static NUSTitle loadNUSTitle(File inputFile, byte[] commonKey) throws IOException, ParserConfigurationException, SAXException, ParseException {
        NUSTitleConfig config = new NUSTitleConfig();

        config.setCommonKey(commonKey);

        WumadInfo wumadInfo = WumadParser.createWumadInfo(inputFile);

        return NUSTitleLoader.loadNusTitle(config, () -> new NUSDataProviderWumad(wumadInfo));
    }

}
