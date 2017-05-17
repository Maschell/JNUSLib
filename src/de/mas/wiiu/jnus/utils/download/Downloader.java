package de.mas.wiiu.jnus.utils.download;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import lombok.extern.java.Log;

@Log
public abstract class Downloader {
    public Downloader() {
        //
    }

    public static byte[] downloadFileToByteArray(String fileURL) throws IOException {
        int BUFFER_SIZE = 0x800;
        URL url = new URL(fileURL);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        int responseCode = httpConn.getResponseCode();

        byte[] file = null;

        if (responseCode == HttpURLConnection.HTTP_OK) {
            int contentLength = httpConn.getContentLength();

            file = new byte[contentLength];

            InputStream inputStream = httpConn.getInputStream();

            int bytesRead = -1;
            byte[] buffer = new byte[BUFFER_SIZE];
            int filePostion = 0;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                System.arraycopy(buffer, 0, file, filePostion, bytesRead);
                filePostion += bytesRead;
            }
            inputStream.close();
        } else {
            log.info("File not found: " + fileURL);
        }
        httpConn.disconnect();
        return file;
    }
}
