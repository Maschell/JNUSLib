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

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;

import de.mas.wiiu.jnus.implementations.wud.WUDImage;
import de.mas.wiiu.jnus.implementations.wud.WUDImageCompressedInfo;
import de.mas.wiiu.jnus.utils.StreamUtils;

public class WUDDiscReaderCompressed extends WUDDiscReader {

    public WUDDiscReaderCompressed(WUDImage image) {
        super(image);
    }

    /**
     * Expects the .wux format by Exzap. You can more infos about it here. https://gbatemp.net/threads/wii-u-image-wud-compression-tool.397901/
     */
    @Override
    public long readEncryptedToStream(OutputStream out, long offset, long size) throws IOException {
        // make sure there is no out-of-bounds read
        WUDImageCompressedInfo info = getImage().getCompressedInfo();

        long fileBytesLeft = info.getUncompressedSize() - offset;

        long usedOffset = offset;
        long usedSize = size;

        if (fileBytesLeft <= 0) {
            throw new IOException("Offset was to big.");
        }
        if (fileBytesLeft < usedSize) {
            usedSize = fileBytesLeft;
        }
        // compressed read must be handled on a per-sector level

        int bufferSize = 0x8000;
        byte[] buffer = new byte[bufferSize];

        RandomAccessFile input = getRandomAccessFileStream();
        try {
            synchronized (input) {
                while (usedSize > 0) {
                    long sectorOffset = (usedOffset % info.getSectorSize());
                    long remainingSectorBytes = info.getSectorSize() - sectorOffset;
                    long sectorIndex = (usedOffset / info.getSectorSize());
                    int bytesToRead = (int) ((remainingSectorBytes < usedSize) ? remainingSectorBytes : usedSize); // read only up to the end of the current
                                                                                                                   // sector
                    // look up real sector index
                    long realSectorIndex = info.getSectorIndex((int) sectorIndex);
                    long offset2 = info.getOffsetSectorArray() + realSectorIndex * info.getSectorSize() + sectorOffset;

                    input.seek(offset2);
                    int read = input.read(buffer);

                    if (read < 0) {
                        break;
                    }
                    try {
                        out.write(Arrays.copyOfRange(buffer, 0, bytesToRead));
                    } catch (IOException e) {
                        if (e.getMessage().equals("Pipe closed")) {
                            break;
                        } else {
                            throw e;
                        }
                    }

                    usedSize -= bytesToRead;
                    usedOffset += bytesToRead;
                }
            }
        } finally {
            StreamUtils.closeAll(input, out);
        }
        return size - usedSize;
    }
}
