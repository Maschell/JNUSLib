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

import de.mas.wiiu.jnus.implementations.NUSDataProviderWoomy;
import de.mas.wiiu.jnus.implementations.woomy.WoomyInfo;
import de.mas.wiiu.jnus.implementations.woomy.WoomyParser;

public final class NUSTitleLoaderWoomy {

    private NUSTitleLoaderWoomy() {

    }

    public static NUSTitle loadNUSTitle(String inputFile) throws IOException, ParserConfigurationException, SAXException, ParseException {
        NUSTitleConfig config = new NUSTitleConfig();

        config.setTicketNeeded(false);
        
        WoomyInfo woomyInfo = WoomyParser.createWoomyInfo(new File(inputFile));

        return NUSTitleLoader.loadNusTitle(config, () -> new NUSDataProviderWoomy(woomyInfo));
    }

}
