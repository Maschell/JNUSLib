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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import de.mas.wiiu.jnus.Settings;
import de.mas.wiiu.jnus.entities.content.Content;
import de.mas.wiiu.jnus.implementations.woomy.WoomyInfo;
import de.mas.wiiu.jnus.implementations.woomy.WoomyZipFile;
import de.mas.wiiu.jnus.interfaces.NUSDataProvider;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.java.Log;

@Log
public class NUSDataProviderWoomy implements NUSDataProvider {
    @Getter private final WoomyInfo woomyInfo;
    @Setter(AccessLevel.PRIVATE) private WoomyZipFile woomyZipFile;

    public NUSDataProviderWoomy(WoomyInfo woomyInfo) {
        this.woomyInfo = woomyInfo;
    }

    @Override
    public InputStream readContentAsStream(@NonNull Content content, long fileOffsetBlock, long size) throws IOException {
        WoomyZipFile zipFile = getSharedWoomyZipFile();
        ZipEntry entry = getWoomyInfo().getContentFiles().get(content.getFilename().toLowerCase());
        if (entry == null) {
            log.warning("Inputstream for " + content.getFilename() + " not found");
            throw new FileNotFoundException("Inputstream for " + content.getFilename() + " not found");
        }
        return zipFile.getInputStream(entry);
    }

    @Override
    public Optional<byte[]> getContentH3Hash(Content content) throws IOException {
        ZipEntry entry = getWoomyInfo().getContentFiles().get(content.getFilename().toLowerCase());
        if (entry != null) {
            WoomyZipFile zipFile = getNewWoomyZipFile();
            byte[] result = zipFile.getEntryAsByte(entry);
            zipFile.close();
            return Optional.of(result);
        }
        return Optional.empty();
    }

    @Override
    public Optional<byte[]> getRawTMD() throws IOException {
        ZipEntry entry = getWoomyInfo().getContentFiles().get(Settings.TMD_FILENAME);
        if (entry == null) {
            log.warning(Settings.TMD_FILENAME + " not found in woomy file");
            throw new FileNotFoundException(Settings.TMD_FILENAME + " not found in woomy file");
        }
        WoomyZipFile zipFile = getNewWoomyZipFile();
        byte[] result = zipFile.getEntryAsByte(entry);
        zipFile.close();
        return Optional.of(result);
    }

    @Override
    public Optional<byte[]> getRawTicket() throws IOException {
        ZipEntry entry = getWoomyInfo().getContentFiles().get(Settings.TICKET_FILENAME);
        if (entry == null) {
            log.warning(Settings.TICKET_FILENAME + " not found in woomy file");
            throw new FileNotFoundException(Settings.TICKET_FILENAME + " not found in woomy file");
        }

        WoomyZipFile zipFile = getNewWoomyZipFile();
        byte[] result = zipFile.getEntryAsByte(entry);
        zipFile.close();
        return Optional.of(result);
    }

    public WoomyZipFile getSharedWoomyZipFile() throws ZipException, IOException {
        if (this.woomyZipFile == null || this.woomyZipFile.isClosed()) {
            this.woomyZipFile = getNewWoomyZipFile();
        }
        return this.woomyZipFile;
    }

    private WoomyZipFile getNewWoomyZipFile() throws ZipException, IOException {
        return new WoomyZipFile(getWoomyInfo().getWoomyFile());
    }

    @Override
    public void cleanup() throws IOException {
        if (this.woomyZipFile != null && this.woomyZipFile.isClosed()) {
            this.woomyZipFile.close();
        }
    }

    @Override
    public Optional<byte[]> getRawCert() throws IOException {
        return Optional.empty();
    }
}
