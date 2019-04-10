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

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import de.mas.wiiu.jnus.entities.TMD;
import de.mas.wiiu.jnus.entities.Ticket;
import de.mas.wiiu.jnus.entities.content.Content;
import de.mas.wiiu.jnus.entities.fst.FST;
import de.mas.wiiu.jnus.interfaces.NUSDataProvider;
import de.mas.wiiu.jnus.utils.StreamUtils;
import de.mas.wiiu.jnus.utils.cryptography.AESDecryption;

public class NUSTitleLoader {
    private NUSTitleLoader() {
        // should be empty
    }

    public static NUSTitle loadNusTitle(NUSTitleConfig config, Supplier<NUSDataProvider> dataProviderFunction)
            throws IOException, ParseException {
        NUSTitle result = new NUSTitle();

        NUSDataProvider dataProvider = dataProviderFunction.get();
        result.setDataProvider(dataProvider);

        byte[] tmdData = dataProvider.getRawTMD().orElseThrow(() -> new ParseException("No TMD data found", 0));

        TMD tmd = TMD.parseTMD(tmdData);
        result.setTMD(tmd);

        if (config.isNoDecryption()) {
            return result;
        }

        Ticket ticket = config.getTicket();
        if (ticket == null) {
            Optional<byte[]> ticketOpt = dataProvider.getRawTicket();
            if (ticketOpt.isPresent()) {             
                ticket = Ticket.parseTicket(ticketOpt.get(), config.getCommonKey());
            }
        }
        if(ticket == null) {
            new ParseException("Failed to get ticket data",0);
        }
        result.setTicket(ticket);

        Content fstContent = tmd.getContentByIndex(0);

        InputStream fstContentEncryptedStream = dataProvider.getInputStreamFromContent(fstContent, 0, Optional.of(fstContent.getEncryptedFileSize()));

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
}
