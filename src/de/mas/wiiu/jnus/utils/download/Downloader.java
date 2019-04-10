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
package de.mas.wiiu.jnus.utils.download;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import de.mas.wiiu.jnus.Settings;
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
        httpConn.setRequestProperty("User-Agent", Settings.USER_AGENT);
        int responseCode = httpConn.getResponseCode();

        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            InputStream inputStream = httpConn.getInputStream();

            int bytesRead = -1;
            byte[] buffer = new byte[BUFFER_SIZE];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArray.write(buffer, 0, bytesRead);
            }
            inputStream.close();
        } else {
            log.fine("File not found: " + fileURL);
        }
        httpConn.disconnect();
        byte[] result = byteArray.toByteArray();
        byteArray.close();
        return result;
    }
}
