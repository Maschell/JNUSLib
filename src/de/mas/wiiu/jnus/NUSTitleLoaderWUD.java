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
import java.util.ArrayList;
import java.util.List;

import de.mas.wiiu.jnus.implementations.NUSDataProvider;
import de.mas.wiiu.jnus.implementations.NUSDataProviderWUD;
import de.mas.wiiu.jnus.implementations.NUSDataProviderWUDGI;
import de.mas.wiiu.jnus.implementations.wud.WUDImage;
import de.mas.wiiu.jnus.implementations.wud.parser.WUDInfo;
import de.mas.wiiu.jnus.implementations.wud.parser.WUDInfoParser;
import lombok.val;
import lombok.extern.java.Log;

@Log
public final class NUSTitleLoaderWUD extends NUSTitleLoader {

    private NUSTitleLoaderWUD() {
        super();
    }

    public static List<NUSTitle> loadNUSTitle(String WUDPath) throws Exception {
        return loadNUSTitle(WUDPath, (byte[]) null);
    }

    public static List<NUSTitle> loadNUSTitle(String WUDPath, File key) throws Exception {
        byte[] data = Files.readAllBytes(key.toPath());
        if (data == null) {
            log.warning("Failed to read the key file.");
            return new ArrayList<>();
        }
        return loadNUSTitle(WUDPath, data);
    }

    public static List<NUSTitle> loadNUSTitleDev(String WUDPath) throws Exception {
        return loadNUSTitle(WUDPath, null, true);
    }

    public static List<NUSTitle> loadNUSTitle(String WUDPath, byte[] titleKey) throws Exception {
        return loadNUSTitle(WUDPath, titleKey, false);
    }

    public static List<NUSTitle> loadNUSTitle(String WUDPath, byte[] titleKey, boolean forceNoKey) throws Exception {
        byte[] usedTitleKey = titleKey;
        File wudFile = new File(WUDPath);
        if (!wudFile.exists()) {
            log.warning(WUDPath + " does not exist.");
            System.exit(1);
        }

        WUDImage image = new WUDImage(wudFile);
        if (usedTitleKey == null && !forceNoKey) {
            File keyFile = new File(wudFile.getParentFile().getPath() + File.separator + Settings.WUD_KEY_FILENAME);
            if (!keyFile.exists()) {
                log.warning(keyFile.getAbsolutePath() + " does not exist and no title key was provided.");
                return new ArrayList<>();
            }
            usedTitleKey = Files.readAllBytes(keyFile.toPath());
        }
        WUDInfo wudInfo = WUDInfoParser.createAndLoad(image.getWUDDiscReader(), usedTitleKey);

        if (wudInfo == null) {
            log.warning("Failed to parse any WUDInfo");
            return new ArrayList<>();
        }

        List<NUSTitle> result = new ArrayList<>();

        for (val gamePartition : wudInfo.getGamePartitions()) {
            NUSTitleConfig config = new NUSTitleConfig();
            NUSTitleLoader loader = new NUSTitleLoaderWUD();

            config.setWUDGamePartition(gamePartition);
            config.setWUDInfo(wudInfo);
            result.add(loader.loadNusTitle(config));
        }

        for (val giPartitionTitle : wudInfo.getGIPartitionTitles()) {
            NUSTitleConfig config = new NUSTitleConfig();
            NUSTitleLoader loader = new NUSTitleLoaderWUD();

            config.setWUDGIPartitionTitle(giPartitionTitle);
            config.setWUDInfo(wudInfo);
            result.add(loader.loadNusTitle(config));
        }
        return result;
    }

    @Override
    protected NUSDataProvider getDataProvider(NUSTitle title, NUSTitleConfig config) {
        if (config.getWUDGIPartitionTitle() != null) {
            return new NUSDataProviderWUDGI(title, config.getWUDGIPartitionTitle(), config.getWUDInfo().getWUDDiscReader(), config.getWUDInfo().getTitleKey());
        }
        return new NUSDataProviderWUD(title, config.getWUDGamePartition(), config.getWUDInfo().getWUDDiscReader());
    }

}
