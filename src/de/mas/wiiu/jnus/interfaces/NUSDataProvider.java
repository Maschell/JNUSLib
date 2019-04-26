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

package de.mas.wiiu.jnus.interfaces;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import de.mas.wiiu.jnus.entities.content.Content;
import de.mas.wiiu.jnus.utils.StreamUtils;

public interface NUSDataProvider {

    default public byte[] getChunkFromContent(Content content, long offset, int size) throws IOException {
        return StreamUtils.getBytesFromStream(getInputStreamFromContent(content, offset, Optional.of((long) size)), size);
    }

    /**
     * 
     * @param content
     * @param offset
     * @return
     * @throws IOException
     */
    public InputStream getInputStreamFromContent(Content content, long offset, Optional<Long> size) throws IOException;

    default public InputStream getInputStreamFromContent(Content content, long offset) throws IOException {
        return getInputStreamFromContent(content, offset, Optional.empty());
    }

    public Optional<byte[]> getContentH3Hash(Content content) throws IOException;

    public Optional<byte[]> getRawTMD() throws IOException;

    public Optional<byte[]> getRawTicket() throws IOException;

    public Optional<byte[]> getRawCert() throws IOException;

    public void cleanup() throws IOException;

}