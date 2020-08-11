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
import java.util.Optional;
import java.util.stream.Collectors;

import de.mas.wiiu.jnus.implementations.DefaultNUSDataProcessor;
import de.mas.wiiu.jnus.implementations.FSTDataProviderNUSTitle;
import de.mas.wiiu.jnus.implementations.FSTDataProviderWUDDataPartition;
import de.mas.wiiu.jnus.implementations.NUSDataProviderWUD;
import de.mas.wiiu.jnus.implementations.wud.WUDImage;
import de.mas.wiiu.jnus.implementations.wud.WiiUDisc;
import de.mas.wiiu.jnus.implementations.wud.content.partitions.WiiUDataPartition;
import de.mas.wiiu.jnus.implementations.wud.content.partitions.WiiUGMPartition;
import de.mas.wiiu.jnus.implementations.wud.reader.WUDDiscReader;
import de.mas.wiiu.jnus.interfaces.FSTDataProvider;
import de.mas.wiiu.jnus.utils.Utils;
import lombok.NonNull;
import lombok.val;
import lombok.var;

public final class WUDLoader {

    private WUDLoader() {
        super();
    }

    public static void main(String[] args) throws IOException, ParseException {
        var disc = WUDLoader.load("H:\\WUD\\Kiosk\\August 2015\\August 2015.wux", null, true);
        getPartitonsAsFSTDataProvider(disc, Utils.StringToByteArray("2f5c1b2944e7fd6fc397964b057691fa"));
    }

    public static WiiUDisc load(String WUDPath) throws IOException, ParseException {
        return load(WUDPath, (byte[]) null);
    }

    public static WiiUDisc load(String WUDPath, File key) throws IOException, ParseException {
        byte[] data = Files.readAllBytes(key.toPath());
        return load(WUDPath, data);
    }

    public static WiiUDisc loadDev(String WUDPath) throws IOException, ParseException {
        return load(WUDPath, null, true);
    }

    public static WiiUDisc load(String WUDPath, byte[] titleKey) throws IOException, ParseException {
        return load(WUDPath, titleKey, false);
    }

    public static WiiUDisc load(String WUDPath, byte[] titleKey, boolean forceNoKey) throws IOException, ParseException {
        Optional<byte[]> usedTitleKey = Optional.empty();
        if (titleKey != null) {
            usedTitleKey = Optional.of(titleKey);
        }
        File wudFile = new File(WUDPath);
        if (!wudFile.exists()) {
            throw new FileNotFoundException(wudFile.getAbsolutePath() + " was not found");
        }

        WUDImage image = new WUDImage(wudFile);
        if (titleKey == null && !forceNoKey) {
            File keyFile = new File(wudFile.getParentFile().getPath() + File.separator + Settings.WUD_KEY_FILENAME);
            if (!keyFile.exists()) {
                throw new FileNotFoundException(keyFile.getAbsolutePath() + " does not exist and no title key was provided.");
            }
            usedTitleKey = Optional.of(Files.readAllBytes(keyFile.toPath()));
        }

        WiiUDisc wiiUDisc = WiiUDisc.parseData(image.getWUDDiscReader(), usedTitleKey);

        return wiiUDisc;
    }

    public static List<NUSTitle> getGamePartionsAsNUSTitles(@NonNull WiiUDisc disc, byte[] commonKey) throws IOException, ParseException {
        List<NUSTitle> result = new ArrayList<>();

        List<WiiUGMPartition> gamePartitions = disc.getHeader().getContentsInformation().getPartitions().stream().filter(p -> p instanceof WiiUGMPartition)
                .map(p -> (WiiUGMPartition) p).collect(Collectors.toList());

        for (val gamePartition : gamePartitions) {
            result.add(convertGamePartitionToNUSTitle(gamePartition, disc.getReader().get(), commonKey));
        }

        return result;
    }

    public static NUSTitle convertGamePartitionToNUSTitle(WiiUGMPartition gamePartition, WUDDiscReader discReader, byte[] commonKey)
            throws IOException, ParseException {
        final NUSTitleConfig config = new NUSTitleConfig();
        config.setCommonKey(commonKey);
        return NUSTitleLoader.loadNusTitle(config, () -> new NUSDataProviderWUD(gamePartition, discReader),
                (dp, cd, en) -> new DefaultNUSDataProcessor(dp, cd));
    }

    public static List<FSTDataProvider> getPartitonsAsFSTDataProvider(@NonNull WiiUDisc disc, byte[] commonKey) throws IOException, ParseException {
        List<FSTDataProvider> result = new ArrayList<>();
        List<WiiUGMPartition> gamePartitions = disc.getHeader().getContentsInformation().getPartitions().stream().filter(p -> p instanceof WiiUGMPartition)
                .map(p -> (WiiUGMPartition) p).collect(Collectors.toList());

        for (val gamePartition : gamePartitions) {
            NUSTitle t = convertGamePartitionToNUSTitle(gamePartition, disc.getReader().get(), commonKey);
            FSTDataProviderNUSTitle res = new FSTDataProviderNUSTitle(t);
            res.setName(gamePartition.getVolumeID());
            result.add(res);
        }

        List<WiiUDataPartition> dataParitions = disc.getHeader().getContentsInformation().getPartitions().stream().filter(p -> p instanceof WiiUDataPartition)
                .map(p -> ((WiiUDataPartition) p)).collect(Collectors.toList());

        for (val partition : dataParitions) {
            result.add(new FSTDataProviderWUDDataPartition(partition, disc.getReader().get(), disc.getDiscKey()));
        }

        return result;
    }

}
