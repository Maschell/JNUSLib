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
package de.mas.wiiu.jnus.implementations;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import de.mas.wiiu.jnus.Settings;
import de.mas.wiiu.jnus.entities.content.Content;
import de.mas.wiiu.jnus.interfaces.NUSDataProvider;
import de.mas.wiiu.jnus.interfaces.Parallelizable;
import de.mas.wiiu.jnus.utils.download.NUSDownloadService;
import lombok.Getter;

public class NUSDataProviderRemote implements NUSDataProvider, Parallelizable {
    @Getter private final int version;
    @Getter private final long titleID;

    public NUSDataProviderRemote(int version, long titleID) {
        this.version = version;
        this.titleID = titleID;
    }

    @Override
    public InputStream getInputStreamFromContent(Content content, long fileOffsetBlock, Optional<Long> size) throws IOException {
        NUSDownloadService downloadService = NUSDownloadService.getDefaultInstance();
        return downloadService.getInputStreamForURL(getRemoteURL(content), fileOffsetBlock, size);
    }

    private String getRemoteURL(Content content) {
        return String.format("%016x/%08X", titleID, content.getID());
    }

    @Override
    public Optional<byte[]> getContentH3Hash(Content content) throws IOException {
        NUSDownloadService downloadService = NUSDownloadService.getDefaultInstance();
        String url = getRemoteURL(content) + Settings.H3_EXTENTION;

        byte[] res = downloadService.downloadToByteArray(url);
        if (res == null || res.length == 0) {
            return Optional.empty();
        }
        return Optional.of(res);
    }

    @Override
    public Optional<byte[]> getRawTMD() throws IOException {
        NUSDownloadService downloadService = NUSDownloadService.getDefaultInstance();

        long titleID = getTitleID();
        int version = getVersion();

        byte[] res = downloadService.downloadTMDToByteArray(titleID, version);

        if (res == null || res.length == 0) {
            return Optional.empty();
        }
        return Optional.of(res);
    }

    @Override
    public Optional<byte[]> getRawTicket() throws IOException {
        NUSDownloadService downloadService = NUSDownloadService.getDefaultInstance();
        byte[] res = downloadService.downloadTicketToByteArray(titleID);
        if (res == null || res.length == 0) {
            return Optional.empty();
        }
        return Optional.of(res);
    }

    @Override
    public Optional<byte[]> getRawCert() throws IOException {
        return Optional.empty();
    }

    @Override
    public void cleanup() throws IOException {
        // We don't need this
    }
}
