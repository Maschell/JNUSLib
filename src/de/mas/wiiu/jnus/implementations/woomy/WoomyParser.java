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
package de.mas.wiiu.jnus.implementations.woomy;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import de.mas.wiiu.jnus.Settings;
import de.mas.wiiu.jnus.implementations.woomy.WoomyMeta.WoomyEntry;
import lombok.NonNull;
import lombok.extern.java.Log;

/**
 * 
 * @author Maschell
 *
 */
@Log
public final class WoomyParser {
    private WoomyParser() {
        //
    }

    public static WoomyInfo createWoomyInfo(File woomyFile) throws IOException, ParserConfigurationException, SAXException {
        WoomyInfo result = new WoomyInfo();
        if (!woomyFile.exists()) {
            log.info("File does not exist." + woomyFile.getAbsolutePath());
            return null;
        }
        try (ZipFile zipFile = new ZipFile(woomyFile)) {
            result.setWoomyFile(woomyFile);
            ZipEntry metaFile = zipFile.getEntry(Settings.WOOMY_METADATA_FILENAME);
            if (metaFile == null) {
                log.info("No meta ");
                return null;
            }
            WoomyMeta meta = WoomyMetaParser.parseMeta(zipFile.getInputStream(metaFile));

            /**
             * Currently we will only use the first entry in the metadata.xml
             */
            if (meta.getEntries().isEmpty()) {
                return null;
            }
            WoomyEntry entry = meta.getEntries().get(0);
            String regEx = entry.getFolder() + ".*"; // We want all files in the entry fodler
            Map<String, ZipEntry> contentFiles = loadFileList(zipFile, regEx);
            result.setContentFiles(contentFiles);

        } catch (ZipException e) {
            log.info("Caught Execption : " + e.getMessage());
        }
        return result;
    }

    private static Map<String, ZipEntry> loadFileList(@NonNull ZipFile zipFile, @NonNull String regEx) {
        Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
        Map<String, ZipEntry> result = new HashMap<>();
        Pattern pattern = Pattern.compile(regEx);
        while (zipEntries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) zipEntries.nextElement();
            if (!entry.isDirectory()) {
                String entryName = entry.getName();
                Matcher matcher = pattern.matcher(entryName);
                if (matcher.matches()) {
                    String[] tokens = entryName.replace(File.separator, "\\").split("[\\\\|/]"); // We only want the filename!
                    String filename = tokens[tokens.length - 1];
                    result.put(filename.toLowerCase(Locale.ENGLISH), entry);
                }
            }
        }
        return result;
    }
}
