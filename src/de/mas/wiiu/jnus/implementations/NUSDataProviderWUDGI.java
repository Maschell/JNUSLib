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

import java.io.IOException;
import java.io.InputStream;

import de.mas.wiiu.jnus.NUSTitle;
import de.mas.wiiu.jnus.Settings;
import de.mas.wiiu.jnus.entities.TMD;
import de.mas.wiiu.jnus.entities.content.Content;
import de.mas.wiiu.jnus.implementations.wud.parser.WUDGIPartitionTitle;
import de.mas.wiiu.jnus.implementations.wud.reader.WUDDiscReader;
import lombok.Getter;

public class NUSDataProviderWUDGI extends NUSDataProvider {
    @Getter private final WUDGIPartitionTitle giPartitionTitle;
    @Getter private final WUDDiscReader discReader;

    private final byte[] titleKey;

    private final TMD tmd;

    public NUSDataProviderWUDGI(NUSTitle title, WUDGIPartitionTitle giPartitionTitle, WUDDiscReader discReader, byte[] titleKey) {
        super(title);
        this.giPartitionTitle = giPartitionTitle;
        this.discReader = discReader;
        this.titleKey = titleKey;
        this.tmd = TMD.parseTMD(getRawTMD());
    }

    @Override
    public InputStream getInputStreamFromContent(Content content, long fileOffsetBlock) throws IOException {
        InputStream in = getGiPartitionTitle().getFileAsStream(content.getFilename(), getDiscReader(), titleKey);
        in.skip(fileOffsetBlock);
        return in;
    }

    @Override
    public byte[] getContentH3Hash(Content content) throws IOException {
        return getGiPartitionTitle().getFileAsByte(String.format("%08X.h3", content.getID()), getDiscReader(), titleKey);
    }

    public TMD getTMD() {
        return tmd;
    }

    @Override
    public byte[] getRawTMD() {
        try {
            return getGiPartitionTitle().getFileAsByte(Settings.TMD_FILENAME, getDiscReader(), titleKey);
        } catch (IOException e) {
            return new byte[0];
        }
    }

    @Override
    public byte[] getRawTicket() {
        try {
            return getGiPartitionTitle().getFileAsByte(Settings.TICKET_FILENAME, getDiscReader(), titleKey);
        } catch (IOException e) {
            return new byte[0];
        }
    }

    @Override
    public byte[] getRawCert() throws IOException {
        try {
            return getGiPartitionTitle().getFileAsByte(Settings.CERT_FILENAME, getDiscReader(), titleKey);
        } catch (IOException e) {
            return new byte[0];
        }
    }

    @Override
    public void cleanup() {
        // We don't need it
    }

}
