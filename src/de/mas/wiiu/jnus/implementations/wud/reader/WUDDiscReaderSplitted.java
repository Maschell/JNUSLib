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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;

import de.mas.wiiu.jnus.implementations.wud.WUDImage;
import lombok.extern.java.Log;

@Log
public class WUDDiscReaderSplitted extends WUDDiscReader {
    public static long WUD_SPLITTED_FILE_SIZE = 0x100000L * 0x800L;
    public static long NUMBER_OF_FILES = 12;
    public static String WUD_SPLITTED_DEFAULT_FILEPATTERN = "game_part%d.wud";

    public WUDDiscReaderSplitted(WUDImage image) {
        super(image);
    }

    @Override
    public void readEncryptedToOutputStream(OutputStream outputStream, long offset, long size) throws IOException {
        RandomAccessFile input = getFileByOffset(offset);

        int bufferSize = 0x8000;
        byte[] buffer = new byte[bufferSize];
        long totalread = 0;
        long curOffset = offset;

        int part = getFilePartByOffset(offset);
        long offsetInFile = getOffsetInFilePart(part, curOffset);

        do {
            offsetInFile = getOffsetInFilePart(part, curOffset);
            int curReadSize = bufferSize;
            if ((offsetInFile + bufferSize) >= WUD_SPLITTED_FILE_SIZE) { // Will we read above the part?
                long toRead = WUD_SPLITTED_FILE_SIZE - offsetInFile;
                if (toRead == 0) { // just load the new file
                    input.close();
                    input = getFileByOffset(curOffset);
                    part++;
                    offsetInFile = getOffsetInFilePart(part, curOffset);
                } else {
                    curReadSize = (int) toRead; // And first only read until the part ends
                }
            }

            int read = input.read(buffer, 0, curReadSize);
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
            curOffset += read;
        } while (totalread < size);

        input.close();
        outputStream.close();
    }

    private int getFilePartByOffset(long offset) {
        return (int) (offset / WUD_SPLITTED_FILE_SIZE) + 1;
    }

    private long getOffsetInFilePart(int part, long offset) {
        return offset - ((long) (part - 1) * WUD_SPLITTED_FILE_SIZE);
    }

    private RandomAccessFile getFileByOffset(long offset) throws IOException {
        File filehandlePart1 = getImage().getFileHandle();
        String pathToFiles = filehandlePart1.getParentFile().getAbsolutePath();

        int filePart = getFilePartByOffset(offset);

        String filePartPath = pathToFiles + File.separator + String.format(WUD_SPLITTED_DEFAULT_FILEPATTERN, filePart);

        File part = new File(filePartPath);

        if (!part.exists()) {
            log.info("File does not exist");
            throw new FileNotFoundException(part.getAbsolutePath() + " does not exist");
        }
        RandomAccessFile result = new RandomAccessFile(part, "r");
        result.seek(getOffsetInFilePart(filePart, offset));
        return result;
    }
}
