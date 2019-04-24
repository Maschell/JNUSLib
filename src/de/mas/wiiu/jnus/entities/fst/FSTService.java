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
package de.mas.wiiu.jnus.entities.fst;

import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import de.mas.wiiu.jnus.entities.content.Content;
import de.mas.wiiu.jnus.entities.content.ContentFSTInfo;
import de.mas.wiiu.jnus.entities.fst.FSTEntry.FSTEntryParam;
import de.mas.wiiu.jnus.utils.ByteUtils;
import lombok.extern.java.Log;

@Log
public final class FSTService {

    private FSTService() {
    }

    public static void parseFST(FSTEntry rootEntry, byte[] fstSection, byte[] namesSection, Map<Integer, Content> contentsByIndex,
            Map<Integer, ContentFSTInfo> contentsFSTByIndex, int sectorSize) throws ParseException {
        int totalEntries = ByteUtils.getIntFromBytes(fstSection, 0x08);

        int level = 0;
        int[] LEntry = new int[16];
        int[] Entry = new int[16];

        final HashMap<Integer, FSTEntry> fstEntryToOffsetMap = new HashMap<>();
        Entry[level] = 0;
        LEntry[level++] = 0;

        fstEntryToOffsetMap.put(0, rootEntry);

        int lastlevel = level;

        for (int i = 1; i < totalEntries; i++) {

            int entryOffset = i;
            if (level > 0) {
                while (LEntry[level - 1] == i) {
                    level--;
                }
            }

            byte[] curEntry = Arrays.copyOfRange(fstSection, i * 0x10, (i + 1) * 0x10);

            FSTEntryParam entryParam = new FSTEntry.FSTEntryParam();

            if (lastlevel != level) {
                lastlevel = level;
            }

            long fileOffset = ByteUtils.getIntFromBytes(curEntry, 0x04);
            long fileSize = ByteUtils.getUnsingedIntFromBytes(curEntry, 0x08);

            short flags = ByteUtils.getShortFromBytes(curEntry, 0x0C);
            short contentIndex = ByteUtils.getShortFromBytes(curEntry, 0x0E);

            if ((curEntry[0] & FSTEntry.FSTEntry_notInNUS) == FSTEntry.FSTEntry_notInNUS) {
                entryParam.setNotInPackage(true);
            }
            FSTEntry parent = null;
            if ((curEntry[0] & FSTEntry.FSTEntry_DIR) == FSTEntry.FSTEntry_DIR) {
                entryParam.setDir(true);
                int parentOffset = (int) fileOffset;
                int nextOffset = (int) fileSize;

                parent = fstEntryToOffsetMap.get(parentOffset);
                Entry[level] = i;
                LEntry[level++] = nextOffset;

                if (level > 15) {
                    log.warning("level > 15");
                    break;
                }
            } else {
                entryParam.setFileOffset(fileOffset * sectorSize);

                entryParam.setFileSize(fileSize);
                parent = fstEntryToOffsetMap.get(Entry[level - 1]);
            }

            entryParam.setFlags(flags);
            final int nameOffset = getNameOffset(curEntry);
            entryParam.setFileNameSupplier(() -> getName(nameOffset, namesSection));

            if (contentsByIndex != null) {
                Content content = contentsByIndex.get((int) contentIndex);
                if (content == null) {
                    if ((!entryParam.isDir() || entryParam.isNotInPackage()) && !contentsByIndex.isEmpty()) {
                        // This is only a problem when the data is NOT on aWUDDataPartition (they have no content files)
                        log.warning("Content for FST Entry not found");     
                        throw new ParseException("Content for FST Entry not found", 0);
                    }
                } else {
                    entryParam.setContent(Optional.of(content));
                    ContentFSTInfo contentFSTInfo = contentsFSTByIndex.get((int) contentIndex);
                    if (contentFSTInfo == null) {
                        log.warning("ContentFSTInfo for FST Entry not found");
                        throw new ParseException("ContentFSTInfo for FST Entry not found", 0);
                    } else {
                        content.setContentFSTInfo(contentFSTInfo);
                    }
                }
            }

            entryParam.setContentFSTID(contentIndex);
            entryParam.setParent(Optional.of(parent));

            FSTEntry entry = new FSTEntry(entryParam);
            fstEntryToOffsetMap.put(entryOffset, entry);
        }

    }

    private static int getNameOffset(byte[] curEntry) {
        // Its a 24bit number. We overwrite the first byte, then we can read it as an Integer.
        // But at first we make a copy.
        byte[] entryData = Arrays.copyOf(curEntry, curEntry.length);
        entryData[0] = 0;
        return ByteUtils.getIntFromBytes(entryData, 0);
    }

    public static String getName(byte[] data, byte[] namesSection) {
        return getName(getNameOffset(data), namesSection);
    }

    public static String getName(int nameOffset, byte[] namesSection) {
        int j = 0;

        while ((nameOffset + j) < namesSection.length && namesSection[nameOffset + j] != 0) {
            j++;
        }

        return (new String(Arrays.copyOfRange(namesSection, nameOffset, nameOffset + j)));
    }

}
