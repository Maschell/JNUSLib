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
package de.mas.wiiu.jnus.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import de.mas.wiiu.jnus.interfaces.CheckedFunction;
import lombok.NonNull;

public final class FileUtils {
    private FileUtils() {
        // Utility Class
    }

    public static boolean saveByteArrayToFile(String filePath, byte[] data) throws IOException {
        File target = new File(filePath);
        if (target.isDirectory()) {
            return false;
        }
        File parent = target.getParentFile();
        if (parent != null) {
            Utils.createDir(parent.getAbsolutePath());
        }
        return saveByteArrayToFile(target, data);
    }

    /**
     * Saves a byte array to a file (and overwrite it if its already exists) DOES NOT IF THE PATH/FILE EXIST OR IS IT EVEN A FILE
     * 
     * @param output
     * @param data
     * @return
     * @throws IOException
     */
    public static boolean saveByteArrayToFile(@NonNull File output, byte[] data) throws IOException {
        FileOutputStream out = new FileOutputStream(output);
        try {
            out.write(data);
        } finally {
            out.close();
        }
        return true;
    }

    public static void saveInputStreamToFile(@NonNull File outputFile, InputStream inputStream, long filesize) throws IOException {
        FileAsOutputStreamWrapper(outputFile, filesize, outputStream -> StreamUtils.saveInputStreamToOutputStream(inputStream, outputStream, filesize));
    }

    /**
     * Allows to write into a target file as OutputStream with some extras. The provided OutputStream already has the needed memory allocated. This results in a
     * non-fragmented file.
     * 
     * @param outputFile
     * @param filesize
     * @param action
     * @throws IOException
     */
    public static void FileAsOutputStreamWrapper(@NonNull File outputFile, long filesize, CheckedFunction<OutputStream> action) throws IOException {
        // Create a new temp file which already has the target filesize allocated.
        String tempFilePath = outputFile.getAbsolutePath() + "." + outputFile.getAbsolutePath().hashCode() + ".part";
        File tempFile = new File(tempFilePath);
        if (tempFile.exists()) {
            tempFile.delete();
        }

        tempFile.createNewFile();
        RandomAccessFile outStream = new RandomAccessFile(tempFilePath, "rw");
        try {
            outStream.setLength(filesize);
            outStream.seek(0L);

            action.apply(new RandomFileOutputStream(outStream));
        } finally {
            outStream.close();
        }

        // Rename temp file.
        if (outputFile.exists()) {
            outputFile.delete();
        }
        tempFile.renameTo(outputFile);
    }

    public static File getFileIgnoringFilenameCases(String folder, String filename) {
        File filepath = new File(folder + File.separator + filename);
        if (!filepath.exists()) {
            // Try to find it ignoring cases.
            File[] filesIngoringCases = new File(folder).listFiles(f -> f.getName().equalsIgnoreCase(filename));
            if (filesIngoringCases != null && filesIngoringCases.length == 1 && !filesIngoringCases[0].isDirectory()) {
                return filesIngoringCases[0].getAbsoluteFile();
            } else {
                return null;
            }
        }
        return filepath;
    }

}
