/****************************************************************************
 * Copyright (C) 2016-2018 Maschell
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import de.mas.wiiu.jnus.NUSTitle;
import de.mas.wiiu.jnus.Settings;
import de.mas.wiiu.jnus.entities.TMD;
import de.mas.wiiu.jnus.entities.content.Content;
import de.mas.wiiu.jnus.utils.Parallelizable;
import de.mas.wiiu.jnus.utils.download.NUSDownloadService;
import lombok.Getter;

public class NUSDataProviderRemote extends NUSDataProvider implements Parallelizable {
    @Getter private final int version;
    @Getter private final long titleID;

    public NUSDataProviderRemote(NUSTitle title, int version, long titleID) {
        super(title);
        this.version = version;
        this.titleID = titleID;
    }

    @Override
    public InputStream getInputStreamFromContent(Content content, long fileOffsetBlock, Optional<Long> size) throws IOException {
        NUSDownloadService downloadService = NUSDownloadService.getDefaultInstance();
        return downloadService.getInputStreamForURL(getRemoteURL(content), fileOffsetBlock, size);
    }

    private String getRemoteURL(Content content) {
        return String.format("%016x/%08X", getNUSTitle().getTMD().getTitleID(), content.getID());
    }

    @Override
    public byte[] getContentH3Hash(Content content) throws IOException {
        NUSDownloadService downloadService = NUSDownloadService.getDefaultInstance();
        String url = getRemoteURL(content) + Settings.H3_EXTENTION;
        return downloadService.downloadToByteArray(url);
    }

    @Override
    public byte[] getRawTMD() throws IOException {
        NUSDownloadService downloadService = NUSDownloadService.getDefaultInstance();

        long titleID = getTitleID();
        int version = getVersion();

        return downloadService.downloadTMDToByteArray(titleID, version);
    }

    @Override
    public byte[] getRawTicket() throws IOException {
        NUSDownloadService downloadService = NUSDownloadService.getDefaultInstance();

        long titleID = getNUSTitle().getTMD().getTitleID();

        return downloadService.downloadTicketToByteArray(titleID);
    }

    @Override
    public byte[] getRawCert() throws IOException {
        NUSDownloadService downloadService = NUSDownloadService.getDefaultInstance();
        byte[] defaultCert = downloadService.downloadDefaultCertToByteArray();

        TMD tmd = getNUSTitle().getTMD();
        byte[] result = new byte[0];
        try {
            ByteArrayOutputStream fos = new ByteArrayOutputStream();
            fos.write(tmd.getCert1());
            fos.write(tmd.getCert2());
            fos.write(defaultCert);
            result = fos.toByteArray();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public void cleanup() throws IOException {
        // We don't need this
    }
}
