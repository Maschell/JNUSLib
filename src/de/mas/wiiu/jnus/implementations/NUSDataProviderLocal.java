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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import de.mas.wiiu.jnus.NUSTitle;
import de.mas.wiiu.jnus.Settings;
import de.mas.wiiu.jnus.entities.content.Content;
import de.mas.wiiu.jnus.utils.StreamUtils;
import lombok.Getter;

public final class NUSDataProviderLocal extends NUSDataProvider {
    @Getter private final String localPath;

    public NUSDataProviderLocal(NUSTitle nustitle, String localPath) {
        super(nustitle);
        this.localPath = localPath;
    }

    public String getFilePathOnDisk(Content c) {
        return getLocalPath() + File.separator + c.getFilename();
    }

    @Override
    public InputStream getInputStreamFromContent(Content content, long offset) throws IOException {
        File filepath = new File(getFilePathOnDisk(content));
        if (!filepath.exists()) {

            return null;
        }
        InputStream in = new FileInputStream(filepath);
        StreamUtils.skipExactly(in, offset);
        return in;
    }

    @Override
    public byte[] getContentH3Hash(Content content) throws IOException {
        String h3Path = getLocalPath() + File.separator + String.format("%08X.h3", content.getID());
        File h3File = new File(h3Path);
        if (!h3File.exists()) {
            return new byte[0];
        }
        return Files.readAllBytes(h3File.toPath());
    }

    @Override
    public byte[] getRawTMD() throws IOException {
        String inputPath = getLocalPath();
        String tmdPath = inputPath + File.separator + Settings.TMD_FILENAME;
        File tmdFile = new File(tmdPath);
        return Files.readAllBytes(tmdFile.toPath());
    }

    @Override
    public byte[] getRawTicket() throws IOException {
        String inputPath = getLocalPath();
        String ticketPath = inputPath + File.separator + Settings.TICKET_FILENAME;
        File ticketFile = new File(ticketPath);
        return Files.readAllBytes(ticketFile.toPath());
    }

    @Override
    public byte[] getRawCert() throws IOException {
        String inputPath = getLocalPath();
        String certPath = inputPath + File.separator + Settings.CERT_FILENAME;
        File certFile = new File(certPath);
        return Files.readAllBytes(certFile.toPath());
    }

    @Override
    public void cleanup() throws IOException {
        // We don't need this
    }

    @Override
    public String toString() {
        return "NUSDataProviderLocal [localPath=" + localPath + "]";
    }
}
