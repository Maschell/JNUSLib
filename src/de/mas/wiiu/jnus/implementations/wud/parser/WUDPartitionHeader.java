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
package de.mas.wiiu.jnus.implementations.wud.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mas.wiiu.jnus.entities.content.Content;
import de.mas.wiiu.jnus.utils.ByteUtils;
import de.mas.wiiu.jnus.utils.HashUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

@Log
public final class WUDPartitionHeader {
    @Getter @Setter private boolean calculatedHashes = false;
    @Getter private final HashMap<Short, byte[]> h3Hashes = new HashMap<>();
    @Getter(AccessLevel.PRIVATE) @Setter(AccessLevel.PRIVATE) private byte[] rawData;

    private WUDPartitionHeader() {
    }

    // TODO: real processing. Currently we are ignoring everything except the hashes
    public static WUDPartitionHeader parseHeader(byte[] header) {
        WUDPartitionHeader result = new WUDPartitionHeader();
        result.setRawData(header);
        return result;
    }

    public void addH3Hashes(short index, byte[] hash) {
        getH3Hashes().put(index, hash);
    }

    public byte[] getH3Hash(Content content) {
        if (content == null) {
            log.info("Can't find h3 hash, given content is null.");
            return null;
        }

        return getH3Hashes().get(content.getIndex());
    }

    public void calculateHashes(Map<Integer, Content> allContents) {
        byte[] header = getRawData();

        // Calculating offset for the hashes
        int cnt = ByteUtils.getIntFromBytes(header, 0x10);
        int start_offset = 0x40 + cnt * 0x04;

        int offset = 0;

        // We have to make sure, that the list is ordered by index
        List<Content> contents = new ArrayList<>(allContents.values());
        Collections.sort(contents, new Comparator<Content>() {
            @Override
            public int compare(Content o1, Content o2) {
                return Short.compare(o1.getIndex(), o2.getIndex());
            }
        });

        for (Content c : allContents.values()) {
            if (!c.isHashed() || !c.isEncrypted()) {
                continue;
            }

            // The encrypted content are splitted in 0x10000 chunk. For each 0x1000 chunk we need one entry in the h3
            int cnt_hashes = (int) (c.getEncryptedFileSize() / 0x10000 / 0x1000) + 1;

            byte[] hash = Arrays.copyOfRange(header, start_offset + offset * 0x14, start_offset + (offset + cnt_hashes) * 0x14);

            // Checking the hash of the h3 file.
            if (!Arrays.equals(HashUtil.hashSHA1(hash), c.getSHA2Hash())) {
                log.info("h3 incorrect from WUD");
            }

            addH3Hashes(c.getIndex(), hash);
            offset += cnt_hashes;
        }

        setCalculatedHashes(true);
    }
}
