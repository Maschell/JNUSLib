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
import java.util.Arrays;

import de.mas.wiiu.jnus.implementations.wud.WUDImage;
import de.mas.wiiu.jnus.utils.StreamUtils;

public class WUDDiscReaderUncompressed extends WUDDiscReader {
    public WUDDiscReaderUncompressed(WUDImage image) {
        super(image);
    }

    @Override
    protected void readEncryptedToOutputStream(OutputStream outputStream, long offset, long size) throws IOException {

        FileInputStream input = new FileInputStream(getImage().getFileHandle());

        StreamUtils.skipExactly(input, offset);

        int bufferSize = 0x8000;
        byte[] buffer = new byte[bufferSize];
        long totalread = 0;
        do {
            int read = input.read(buffer);
            if (read < 0) break;
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
    }

}
