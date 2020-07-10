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

import de.mas.wiiu.jnus.entities.fst.FSTEntry.FSTEntryParam;
import de.mas.wiiu.jnus.utils.ByteUtils;

public final class FSTService {

    private FSTService() {
    }

    public static void parseFST(FSTEntry rootEntry, byte[] fstSection, byte[] namesSection, int sectorSize) throws ParseException {
        int totalEntries = ByteUtils.getIntFromBytes(fstSection, 0x08);

        final Map<Integer, byte[]> data = new HashMap<>();

        for (int i = 1; i < totalEntries; i++) {
            data.put(i, Arrays.copyOfRange(fstSection, i * 0x10, (i + 1) * 0x10));
        }

        parseData(1, totalEntries, rootEntry, data, namesSection, sectorSize);

    }

    private static int parseData(int i, int end, FSTEntry parent, Map<Integer, byte[]> data, byte[] namesSection, int sectorSize) throws ParseException {
        while (i < end) {
            byte[] curEntry = data.get(i);

            FSTEntryParam entryParam = new FSTEntry.FSTEntryParam();

            entryParam.setParent(Optional.of(parent));

            long fileOffset = ByteUtils.getIntFromBytes(curEntry, 0x04);
            long fileSize = ByteUtils.getUnsingedIntFromBytes(curEntry, 0x08);
            short flags = ByteUtils.getShortFromBytes(curEntry, 0x0C);
            short contentIndex = ByteUtils.getShortFromBytes(curEntry, 0x0E);

            if ((curEntry[0] & FSTEntry.FSTEntry_notInNUS) == FSTEntry.FSTEntry_notInNUS) {
                entryParam.setNotInPackage(true);
            }
            if ((curEntry[0] & FSTEntry.FSTEntry_DIR) == FSTEntry.FSTEntry_DIR) {
                entryParam.setDir(true);
            } else {
                entryParam.setFileOffset(fileOffset * sectorSize);
                entryParam.setFileSize(fileSize);
            }

            entryParam.setFlags(flags);
            entryParam.setContentIndex(contentIndex);

            final int nameOffset = getNameOffset(curEntry);
            entryParam.setFileNameSupplier(() -> getName(nameOffset, namesSection));

            FSTEntry entry = new FSTEntry(entryParam);

            parent.addChildren(entry);

            if (entryParam.isDir()) {
                i = parseData(i + 1, (int) fileSize, entry, data, namesSection, sectorSize);
            } else {
                i++;
            }
        }
        return i;

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
