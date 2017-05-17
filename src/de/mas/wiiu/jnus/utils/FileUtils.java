package de.mas.wiiu.jnus.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
        out.write(data);
        out.close();
        return true;
    }

    /**
     * Saves a byte array to a file (and overwrite it if its already exists) DOES NOT IF THE PATH/FILE EXIST OR IS IT EVEN A FILE
     * 
     * @param output
     * @param inputStream
     * @throws IOException
     */
    public static void saveInputStreamToFile(@NonNull File output, InputStream inputStream, long filesize) throws IOException {
        FileOutputStream out = new FileOutputStream(output);
        StreamUtils.saveInputStreamToOutputStream(inputStream, out, filesize);
    }

}
