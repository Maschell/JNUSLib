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
package de.mas.wiiu.jnus;

import java.io.InputStream;
import java.util.Map;

import de.mas.wiiu.jnus.entities.TMD;
import de.mas.wiiu.jnus.entities.Ticket;
import de.mas.wiiu.jnus.entities.content.Content;
import de.mas.wiiu.jnus.entities.fst.FST;
import de.mas.wiiu.jnus.implementations.NUSDataProvider;
import de.mas.wiiu.jnus.utils.StreamUtils;
import de.mas.wiiu.jnus.utils.cryptography.AESDecryption;
import lombok.extern.java.Log;

@Log
abstract class NUSTitleLoader {
    protected NUSTitleLoader() {
        // should be empty
    }

    public NUSTitle loadNusTitle(NUSTitleConfig config) throws Exception {
        NUSTitle result = new NUSTitle();

        NUSDataProvider dataProvider = getDataProvider(result, config);
        result.setDataProvider(dataProvider);

        TMD tmd = TMD.parseTMD(dataProvider.getRawTMD());
        result.setTMD(tmd);

        if (tmd == null) {
            log.info("TMD not found.");
            throw new Exception();
        }

        Ticket ticket = config.getTicket();
        if (ticket == null) {
            ticket = Ticket.parseTicket(dataProvider.getRawTicket());
        }
        result.setTicket(ticket);
        // System.out.println(ticket);

        Content fstContent = tmd.getContentByIndex(0);

        InputStream fstContentEncryptedStream = dataProvider.getInputStreamFromContent(fstContent, 0);
        if (fstContentEncryptedStream == null) {

            return null;
        }

        byte[] fstBytes = StreamUtils.getBytesFromStream(fstContentEncryptedStream, (int) fstContent.getEncryptedFileSize());

        if (fstContent.isEncrypted()) {
            AESDecryption aesDecryption = new AESDecryption(ticket.getDecryptedKey(), new byte[0x10]);
            fstBytes = aesDecryption.decrypt(fstBytes);
        }

        Map<Integer, Content> contents = tmd.getAllContents();

        FST fst = FST.parseFST(fstBytes, contents);
        result.setFST(fst);

        return result;
    }

    protected abstract NUSDataProvider getDataProvider(NUSTitle title, NUSTitleConfig config);
}
