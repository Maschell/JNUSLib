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
import java.nio.file.Files;

import de.mas.wiiu.jnus.implementations.NUSDataProvider;
import de.mas.wiiu.jnus.implementations.NUSDataProviderWUD;
import de.mas.wiiu.jnus.implementations.wud.WUDImage;
import de.mas.wiiu.jnus.implementations.wud.parser.WUDInfo;
import de.mas.wiiu.jnus.implementations.wud.parser.WUDInfoParser;
import lombok.extern.java.Log;

@Log
public final class NUSTitleLoaderWUD extends NUSTitleLoader {

    private NUSTitleLoaderWUD() {
        super();
    }

    public static NUSTitle loadNUSTitle(String WUDPath) throws Exception {
        return loadNUSTitle(WUDPath, null);
    }

    public static NUSTitle loadNUSTitle(String WUDPath, byte[] titleKey) throws Exception {
        NUSTitleLoader loader = new NUSTitleLoaderWUD();
        NUSTitleConfig config = new NUSTitleConfig();
        byte[] usedTitleKey = titleKey;
        File wudFile = new File(WUDPath);
        if (!wudFile.exists()) {
            log.info(WUDPath + " does not exist.");
            System.exit(1);
        }

        WUDImage image = new WUDImage(wudFile);
        if (usedTitleKey == null) {
            File keyFile = new File(wudFile.getParentFile().getPath() + File.separator + Settings.WUD_KEY_FILENAME);
            if (!keyFile.exists()) {
                log.info(keyFile.getAbsolutePath() + " does not exist and no title key was provided.");
                return null;
            }
            usedTitleKey = Files.readAllBytes(keyFile.toPath());
        }
        WUDInfo wudInfo = WUDInfoParser.createAndLoad(image.getWUDDiscReader(), usedTitleKey);
        if (wudInfo == null) {
            return null;
        }

        config.setWUDInfo(wudInfo);

        return loader.loadNusTitle(config);
    }

    @Override
    protected NUSDataProvider getDataProvider(NUSTitle title, NUSTitleConfig config) {
        return new NUSDataProviderWUD(title, config.getWUDInfo());
    }
}
