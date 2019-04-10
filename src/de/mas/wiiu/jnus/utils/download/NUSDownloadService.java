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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import de.mas.wiiu.jnus.Settings;

public final class NUSDownloadService extends Downloader {
    private static NUSDownloadService defaultInstance = new NUSDownloadService(Settings.URL_BASE);
    private static Map<String, NUSDownloadService> instances = new HashMap<>();

    private final String URL_BASE;

    private byte[] defaultCertTicket = null;

    private NUSDownloadService(String URL) {
        this.URL_BASE = URL;
    }

    public static NUSDownloadService getDefaultInstance() {
        return defaultInstance;
    }

    public static NUSDownloadService getInstance(String URL) {
        if (!instances.containsKey(URL)) {
            NUSDownloadService instance = new NUSDownloadService(URL);
            instances.put(URL, instance);
        }
        return instances.get(URL);
    }

    public byte[] getDefaultCert() throws IOException {
        if (defaultCertTicket == null) {
            byte[] ticket = downloadTicketToByteArray(0x000500101000400AL); // Downloading cetk from OSv10
            defaultCertTicket = Arrays.copyOfRange(ticket, 0x350, 0x350 + 0x300);
        }
        return defaultCertTicket;
    }

    public byte[] downloadTMDToByteArray(long titleID, int version) throws IOException {
        String version_suf = "";
        if (version > Settings.LATEST_TMD_VERSION) version_suf = "." + version;
        String URL = URL_BASE + "/" + String.format("%016X", titleID) + "/tmd" + version_suf;
        return downloadFileToByteArray(URL);
    }

    /**
     * For the cert we need a part from a ticket. For this we simply load a public ticket and use it.
     */
    public byte[] downloadDefaultCertToByteArray() throws IOException {
        return getDefaultCert();
    }

    public byte[] downloadTicketToByteArray(long titleID) throws IOException {
        String URL = URL_BASE + "/" + String.format("%016X", titleID) + "/cetk";
        return downloadFileToByteArray(URL);
    }

    public byte[] downloadToByteArray(String url) throws IOException {
        String URL = URL_BASE + "/" + url;
        return downloadFileToByteArray(URL);
    }

    public InputStream getInputStream(String URL, long offset, Optional<Long> size) throws IOException {
        URL url_obj = new URL(URL);
        HttpURLConnection connection = (HttpURLConnection) url_obj.openConnection();
        connection.setRequestProperty("User-Agent", Settings.USER_AGENT);
        String sizeString = "";
        if (size.isPresent()) {
            sizeString = Long.toString(size.get());
        }
        connection.setRequestProperty("Range", "bytes=" + offset + "-" + sizeString);
        try {
            connection.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return connection.getInputStream();
    }

    public InputStream getInputStreamForURL(String url, long offset, Optional<Long> size) throws IOException {
        String URL = URL_BASE + "/" + url;

        return getInputStream(URL, offset, size);
    }
}
