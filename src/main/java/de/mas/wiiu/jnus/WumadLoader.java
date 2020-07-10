package de.mas.wiiu.jnus;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import de.mas.wiiu.jnus.implementations.DefaultNUSDataProcessor;
import de.mas.wiiu.jnus.implementations.FSTDataProviderNUSTitle;
import de.mas.wiiu.jnus.implementations.FSTDataProviderWumadDataPartition;
import de.mas.wiiu.jnus.implementations.NUSDataProviderWumad;
import de.mas.wiiu.jnus.implementations.wud.wumad.WumadGamePartition;
import de.mas.wiiu.jnus.implementations.wud.wumad.WumadInfo;
import de.mas.wiiu.jnus.implementations.wud.wumad.WumadParser;
import de.mas.wiiu.jnus.interfaces.FSTDataProvider;
import lombok.NonNull;
import lombok.val;

public class WumadLoader {

    public static WumadInfo load(File wumadFile) throws IOException, ParserConfigurationException, SAXException, ParseException {
        return WumadParser.createWumadInfo(wumadFile);
    }

    public static List<NUSTitle> getGamePartionsAsNUSTitles(@NonNull WumadInfo wumadInfo, byte[] commonKey) throws IOException, ParseException {
        List<NUSTitle> result = new ArrayList<>();
        for (val gamePartition : wumadInfo.getGamePartitions()) {
            result.add(convertGamePartitionToNUSTitle(gamePartition, wumadInfo.getZipFile(), commonKey));
        }
        return result;
    }

    private static NUSTitle convertGamePartitionToNUSTitle(WumadGamePartition gamePartition, ZipFile wudmadFile, byte[] commonKey)
            throws IOException, ParseException {
        final NUSTitleConfig config = new NUSTitleConfig();
        config.setCommonKey(commonKey);
        gamePartition.getTmd();
        return NUSTitleLoader.loadNusTitle(config, () -> new NUSDataProviderWumad(gamePartition, wudmadFile), (dp, cd, ce) -> new DefaultNUSDataProcessor(dp, cd));
    }

    public static List<FSTDataProvider> getPartitonsAsFSTDataProvider(@NonNull WumadInfo wumadInfo, byte[] commonKey) throws IOException, ParseException {
        List<FSTDataProvider> result = new ArrayList<>();
        for (val gamePartition : wumadInfo.getGamePartitions()) {
            NUSTitle t = convertGamePartitionToNUSTitle(gamePartition, wumadInfo.getZipFile(), commonKey);
            FSTDataProviderNUSTitle res = new FSTDataProviderNUSTitle(t);
            res.setName(gamePartition.getPartitionName());
            result.add(res);
        }

        for (val dataPartition : wumadInfo.getDataPartitions()) {
            result.add(new FSTDataProviderWumadDataPartition(dataPartition, wumadInfo.getZipFile()));
        }

        return result;
    }

}
