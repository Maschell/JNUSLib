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
import java.io.OutputStream;
import java.util.stream.Collectors;

import de.mas.wiiu.jnus.NUSTitle;
import de.mas.wiiu.jnus.entities.content.Content;
import de.mas.wiiu.jnus.entities.fst.FSTEntry;
import de.mas.wiiu.jnus.interfaces.FSTDataProvider;
import de.mas.wiiu.jnus.interfaces.HasNUSTitle;
import de.mas.wiiu.jnus.interfaces.NUSDataProcessor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

@Log
public class FSTDataProviderNUSTitle implements FSTDataProvider, HasNUSTitle {
    private final NUSDataProcessor dataProcessor;
    private final NUSTitle title;
    private final FSTEntry rootEntry;
    @Getter @Setter private String name;

    public FSTDataProviderNUSTitle(NUSTitle title) throws IOException {
        this.dataProcessor = title.getDataProcessor();
        this.title = title;
        this.name = String.format("%016X", title.getTMD().getTitleID());

        if (title.getFST().isPresent()) {
            rootEntry = title.getFST().get().getRoot();
        } else if (title.getTMD().getContentCount() == 1) {
            // If the tmd has only one content file, it has not FST. We have to create our own FST.
            Content c = title.getTMD().getAllContents().values().stream().collect(Collectors.toList()).get(0);
            FSTEntry root = FSTEntry.getRootFSTEntry();
            root.addChildren(FSTEntry.createFSTEntry(root, "data.bin", c));
            rootEntry = root;
        } else {
            throw new IOException("No FST root entry was found");
        }
    }
    
    @Override
    public FSTEntry getRoot() {
        return rootEntry;
    }

    @Override
    public long readFileToStream(OutputStream out, FSTEntry entry, long offset, long size) throws IOException {
        if (entry.isNotInPackage()) {
            if (entry.isNotInPackage()) {
                log.info("Decryption not possible because the FSTEntry is not in this package");
            }
            out.close();
            return -1;
        }

        Content c = title.getTMD().getContentByIndex(entry.getContentIndex());

        return dataProcessor.readPlainDecryptedContentToStream(out, c, offset + entry.getFileOffset(), size, size == entry.getFileSize());
    }

    @Override
    public NUSTitle getNUSTitle() {
        return title;
    }
}
