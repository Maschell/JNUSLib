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
package de.mas.wiiu.jnus.implementations.wud.reader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;

import de.mas.wiiu.jnus.implementations.wud.WUDImage;
import de.mas.wiiu.jnus.utils.ByteUtils;
import de.mas.wiiu.jnus.utils.StreamUtils;
import lombok.val;

public class WUDDiscReaderWumadArchive extends WUDDiscReader {
    private final Map<Long, WUMADAOffsetInfo> offsetMap = new TreeMap<>();
    private final int WUMADA_HEADER_SIZE = 0x10000;

    public WUDDiscReaderWumadArchive(WUDImage image, long baseOffset) throws IOException {
        super(image, baseOffset);

        FileInputStream input = new FileInputStream(getImage().getFileHandle());
        StreamUtils.skipExactly(input, 0 + this.getBaseOffset());

        byte[] rawHeader = StreamUtils.getBytesFromStream(input, WUMADA_HEADER_SIZE);

        int fileTableOffset = 0x1000;
        int sizeSectorSize = 0x8000;
        int offsetSectorSize = 0x800;

        long curOffsetInFile = fileTableOffset;
        long curOffset = WUMADA_HEADER_SIZE;

        // Mapping.
        while (true) {
            long curValue = ByteUtils.getUnsingedIntFromBytes(rawHeader, (int) curOffsetInFile, ByteOrder.LITTLE_ENDIAN);
            if (curValue == 0 && curOffsetInFile != fileTableOffset) {
                break;
            }
            curOffsetInFile += 4;
            long size = ByteUtils.getUnsingedIntFromBytes(rawHeader, (int) curOffsetInFile, ByteOrder.LITTLE_ENDIAN);
            curOffsetInFile += 4;

            offsetMap.put(curValue * offsetSectorSize, new WUMADAOffsetInfo(curOffset, size * sizeSectorSize, curValue * offsetSectorSize, false));
            curOffset += size * sizeSectorSize;
        }

        // Fill in empty regions
        long calculatedOffset = 0;
        Map<Long, WUMADAOffsetInfo> offsetMapEmptyRegions = new TreeMap<>();
        long lastEntry = 0;
        for (val curEntry : offsetMap.entrySet()) {
            long offsetOnDisc = curEntry.getKey();
            if (offsetOnDisc != calculatedOffset) {
                offsetMapEmptyRegions.put(calculatedOffset, new WUMADAOffsetInfo(0, offsetOnDisc - calculatedOffset, calculatedOffset, true));
                calculatedOffset += offsetOnDisc - calculatedOffset;
            }
            calculatedOffset += curEntry.getValue().getSize();
            lastEntry = calculatedOffset;
        }
        if (lastEntry < WUDImage.WUD_FILESIZE) {
            offsetMap.put(lastEntry, new WUMADAOffsetInfo(0, WUDImage.WUD_FILESIZE - lastEntry, lastEntry, true));
        }
        offsetMap.putAll(offsetMapEmptyRegions);
    }

    @Override
    public long readEncryptedToStream(OutputStream outputStream, long offset, long size) throws IOException {
        FileInputStream input = new FileInputStream(getImage().getFileHandle());

        WUMADAOffsetInfo curOffsetInfo = getOffsetInfoForOffset(offset).orElseThrow(() -> new IOException("Invalid read"));

        long targetOffsetOfSectionStart = curOffsetInfo.getTargetOffset();
        long inSectionOffset = offset - targetOffsetOfSectionStart;

        if (inSectionOffset > curOffsetInfo.getSize()) {
            throw new IOException("offset > size");
        }

        long realOffset = inSectionOffset + curOffsetInfo.getOffset();

        StreamUtils.skipExactly(input, realOffset + this.getBaseOffset());

        int bufferSize = 0x8000;
        byte[] buffer = new byte[bufferSize];
        long totalread = 0;
        long readInSection = 0;
        long maximumToRead = curOffsetInfo.getSize() - inSectionOffset;
        do {
            int read = 0;
            if (curOffsetInfo.isEmptyContent()) {
                Arrays.fill(buffer, (byte) 0);
                read = bufferSize;
            } else {
                read = input.read(buffer);
                if (read < 0) {
                    break;
                }
            }
            readInSection += read;
            // System.out.println(readInSection + " read in section " + maximumToRead);

            if (readInSection >= maximumToRead) {

                read -= (int) (readInSection - maximumToRead);
                long offsetRead = offset + totalread + read;
                if (offsetRead < offset + size) {
                    curOffsetInfo = getOffsetInfoForOffset(offsetRead).orElseThrow(() -> new IOException("Invalid read for offset " + offsetRead));
                    maximumToRead = curOffsetInfo.getSize();
                    readInSection = 0;

                    input.close();
                    input = new FileInputStream(getImage().getFileHandle());
                    StreamUtils.skipExactly(input, curOffsetInfo.getOffset() + this.getBaseOffset());
                }

            }

            if (totalread + read > size) {
                read = (int) (size - totalread);
            }
            try {
                outputStream.write(Arrays.copyOfRange(buffer, 0, read));
            } catch (IOException e) {
                if (e.getMessage().equals("Pipe closed")) {
                    break;
                } else {
                    input.close();
                    throw e;
                }
            }
            totalread += read;
        } while (totalread < size);
        input.close();
        outputStream.close();
        return totalread;
    }

    private Optional<WUMADAOffsetInfo> getOffsetInfoForOffset(long offset) {
        Optional<WUMADAOffsetInfo> curOffsetInfo = Optional.empty();
        for (Entry<Long, WUMADAOffsetInfo> test : offsetMap.entrySet()) {
            val start = test.getValue().getTargetOffset();
            val end = start + test.getValue().getSize();

            if (offset < start) {
                continue;
            }

            if (offset < end) {
                curOffsetInfo = Optional.of(test.getValue());
                break;
            }
        }
        return curOffsetInfo;
    }

}
