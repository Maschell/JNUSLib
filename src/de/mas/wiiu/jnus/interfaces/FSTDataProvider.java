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
import java.io.OutputStream;
import java.io.PipedOutputStream;
import java.util.Optional;

import de.mas.wiiu.jnus.entities.fst.FSTEntry;
import de.mas.wiiu.jnus.utils.PipedInputStreamWithException;

public interface FSTDataProvider {
    public String getName();

    public FSTEntry getRoot();

    public default byte[] readFile(FSTEntry entry) throws IOException {
        return readFile(entry, 0, entry.getFileSize());
    }

    public byte[] readFile(FSTEntry entry, long offset, long size) throws IOException;

    
    default public InputStream readFileAsStream(FSTEntry entry) throws IOException {
        return readFileAsStream(entry, 0, Optional.empty());
    }
    
    default public InputStream readFileAsStream(FSTEntry entry, long offset, Optional<Long> size) throws IOException {
        PipedInputStreamWithException in = new PipedInputStreamWithException();
        PipedOutputStream out = new PipedOutputStream(in);

        new Thread(() -> {
            try {
                readFileToStream(out, entry, offset, size);
                in.throwException(null);
            } catch (Exception e) {
                in.throwException(e);
            }
        }).start();

        return in;
    }

    default public boolean readFileToStream(OutputStream out, FSTEntry entry, long offset) throws IOException {
        return readFileToStream(out, entry, offset, Optional.empty());
    }

    public boolean readFileToStream(OutputStream out, FSTEntry entry, long offset, Optional<Long> size) throws IOException;

   

}
