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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Optional;

import de.mas.wiiu.jnus.NUSTitle;
import de.mas.wiiu.jnus.Settings;
import de.mas.wiiu.jnus.entities.content.Content;
import de.mas.wiiu.jnus.utils.FileUtils;
import de.mas.wiiu.jnus.utils.StreamUtils;
import lombok.Getter;
import lombok.extern.java.Log;

@Log
public final class NUSDataProviderLocal implements NUSDataProvider {
    @Getter private final String localPath;

    public NUSDataProviderLocal(String localPath) {
        this.localPath = localPath;
    }

    public String getFilePathOnDisk(Content c) {
        return getLocalPath() + File.separator + c.getFilename();
    }

    @Override
    public InputStream getInputStreamFromContent(Content content, long offset, Optional<Long> size) throws IOException {
        File filepath = FileUtils.getFileIgnoringFilenameCases(getLocalPath(), content.getFilename());
        if (filepath == null || !filepath.exists()) {
            String errormsg = "Couldn't open \"" + getLocalPath() + File.separator + content.getFilename() + "\", file does not exist";
            log.warning(errormsg);
            throw new FileNotFoundException(errormsg);
        }
        InputStream in = new FileInputStream(filepath);
        StreamUtils.skipExactly(in, offset);
        return in;
    }

    @Override
    public Optional<byte[]> getContentH3Hash(Content content) throws IOException {
        String h3Filename = String.format("%08X%s", content.getID(), Settings.H3_EXTENTION);
        File filepath = FileUtils.getFileIgnoringFilenameCases(getLocalPath(), h3Filename);
        if (filepath == null || !filepath.exists()) {
            String errormsg = "Couldn't open \"" + getLocalPath() + File.separator + h3Filename + "\", file does not exist";
            log.warning(errormsg);
            throw new FileNotFoundException(errormsg);
        }
        return Optional.of(Files.readAllBytes(filepath.toPath()));
    }

    @Override
    public Optional<byte[]> getRawTMD() throws IOException {
        File file = FileUtils.getFileIgnoringFilenameCases(getLocalPath(), Settings.TMD_FILENAME);
        if (file == null || !file.exists()) {
            String errormsg = "Couldn't open \"" + getLocalPath() + File.separator + Settings.TMD_FILENAME + "\", file does not exist";
            log.warning(errormsg);
            throw new FileNotFoundException(errormsg);
        }
        return Optional.of(Files.readAllBytes(file.toPath()));
    }

    @Override
    public Optional<byte[]> getRawTicket() throws IOException {
        File file = FileUtils.getFileIgnoringFilenameCases(getLocalPath(), Settings.TICKET_FILENAME);
        if (file == null || !file.exists()) {
            String errormsg = "Couldn't open \"" + getLocalPath() + File.separator + Settings.TICKET_FILENAME + "\", file does not exist";
            log.warning(errormsg);
            throw new FileNotFoundException(errormsg);
        }
        return Optional.of(Files.readAllBytes(file.toPath()));
    }

    @Override
    public Optional<byte[]> getRawCert() throws IOException {
        File file = FileUtils.getFileIgnoringFilenameCases(getLocalPath(), Settings.CERT_FILENAME);
        if (file == null || !file.exists()) {
            String errormsg = "Couldn't open \"" + getLocalPath() + File.separator + Settings.CERT_FILENAME + "\", file does not exist";
            log.warning(errormsg);
            throw new FileNotFoundException(errormsg);
        }
        return Optional.of(Files.readAllBytes(file.toPath()));
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
