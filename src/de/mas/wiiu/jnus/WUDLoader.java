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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import de.mas.wiiu.jnus.implementations.FSTDataProviderNUSTitle;
import de.mas.wiiu.jnus.implementations.FSTDataProviderWUDDataPartition;
import de.mas.wiiu.jnus.implementations.NUSDataProviderWUD;
import de.mas.wiiu.jnus.implementations.wud.WUDImage;
import de.mas.wiiu.jnus.implementations.wud.parser.WUDGamePartition;
import de.mas.wiiu.jnus.implementations.wud.parser.WUDInfo;
import de.mas.wiiu.jnus.implementations.wud.parser.WUDInfoParser;
import de.mas.wiiu.jnus.implementations.wud.reader.WUDDiscReader;
import de.mas.wiiu.jnus.interfaces.FSTDataProvider;
import lombok.NonNull;
import lombok.val;

public final class WUDLoader {

    private WUDLoader() {
        super();
    }

    public static WUDInfo load(String WUDPath) throws IOException, ParseException {
        return load(WUDPath, (byte[]) null);
    }

    public static WUDInfo load(String WUDPath, File key) throws IOException, ParseException {
        byte[] data = Files.readAllBytes(key.toPath());
        return load(WUDPath, data);
    }

    public static WUDInfo loadDev(String WUDPath) throws IOException, ParseException {
        return load(WUDPath, null, true);
    }

    public static WUDInfo load(String WUDPath, byte[] titleKey) throws IOException, ParseException {
        return load(WUDPath, titleKey, false);
    }

    public static WUDInfo load(String WUDPath, byte[] titleKey, boolean forceNoKey) throws IOException, ParseException {
        byte[] usedTitleKey = titleKey;
        File wudFile = new File(WUDPath);
        if (!wudFile.exists()) {
            throw new FileNotFoundException(wudFile.getAbsolutePath() + " was not found");
        }

        WUDImage image = new WUDImage(wudFile);
        if (usedTitleKey == null && !forceNoKey) {
            File keyFile = new File(wudFile.getParentFile().getPath() + File.separator + Settings.WUD_KEY_FILENAME);
            if (!keyFile.exists()) {
                throw new FileNotFoundException(keyFile.getAbsolutePath() + " does not exist and no title key was provided.");
            }
            usedTitleKey = Files.readAllBytes(keyFile.toPath());
        }

        WUDInfo wudInfo = WUDInfoParser.createAndLoad(image.getWUDDiscReader(), usedTitleKey);

        return wudInfo;
    }

    public static List<NUSTitle> getGamePartionsAsNUSTitles(@NonNull WUDInfo wudInfo, byte[] commonKey) throws IOException, ParseException {
        List<NUSTitle> result = new ArrayList<>();

        for (val gamePartition : wudInfo.getGamePartitions()) {
            result.add(convertGamePartitionToNUSTitle(gamePartition, wudInfo.getWUDDiscReader(), commonKey));
        }

        return result;
    }

    public static NUSTitle convertGamePartitionToNUSTitle(WUDGamePartition gamePartition, WUDDiscReader discReader, byte[] commonKey)
            throws IOException, ParseException {
        final NUSTitleConfig config = new NUSTitleConfig();
        config.setCommonKey(commonKey);
        gamePartition.getTmd();
        return NUSTitleLoader.loadNusTitle(config, () -> new NUSDataProviderWUD(gamePartition, discReader));
    }

    public static List<FSTDataProvider> getPartitonsAsFSTDataProvider(@NonNull WUDInfo wudInfo, byte[] commonKey) throws IOException, ParseException {
        List<FSTDataProvider> result = new ArrayList<>();
        for (val gamePartition : wudInfo.getGamePartitions()) {
            NUSTitle t = convertGamePartitionToNUSTitle(gamePartition, wudInfo.getWUDDiscReader(), commonKey);
            FSTDataProviderNUSTitle res = new FSTDataProviderNUSTitle(t);
            res.setName(gamePartition.getPartitionName());
            result.add(res);
        }

        for (val partition : wudInfo.getDataPartitions()) {
            result.add(new FSTDataProviderWUDDataPartition(partition, wudInfo.getWUDDiscReader(), wudInfo.getTitleKey()));
        }

        return result;

    }

}
