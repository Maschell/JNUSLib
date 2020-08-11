/****************************************************************************
 * Copyright (C) 2016-2020 Maschell
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchProviderException;
import java.text.ParseException;
import java.util.Optional;
import java.util.function.Supplier;

import de.mas.wiiu.jnus.entities.Ticket;
import de.mas.wiiu.jnus.entities.FST.FST;
import de.mas.wiiu.jnus.entities.FST.nodeentry.FileEntry;
import de.mas.wiiu.jnus.entities.FST.nodeentry.NodeEntry;
import de.mas.wiiu.jnus.entities.TMD.Content;
import de.mas.wiiu.jnus.entities.TMD.TitleMetaData;
import de.mas.wiiu.jnus.implementations.FSTDataProviderNUSTitle;
import de.mas.wiiu.jnus.interfaces.ContentDecryptor;
import de.mas.wiiu.jnus.interfaces.ContentEncryptor;
import de.mas.wiiu.jnus.interfaces.FSTDataProvider;
import de.mas.wiiu.jnus.interfaces.NUSDataProcessor;
import de.mas.wiiu.jnus.interfaces.NUSDataProvider;
import de.mas.wiiu.jnus.interfaces.TriFunction;
import de.mas.wiiu.jnus.utils.cryptography.NUSDecryption;
import de.mas.wiiu.jnus.utils.cryptography.NUSEncryption;

public class NUSTitleLoader {
    private NUSTitleLoader() {
        // should be empty
    }

    public static NUSTitle loadNusTitle(NUSTitleConfig config, Supplier<NUSDataProvider> dataProviderFunction,
            TriFunction<NUSDataProvider, Optional<ContentDecryptor>, Optional<ContentEncryptor>, NUSDataProcessor> dataProcessorFunction)
            throws IOException, ParseException {
        NUSDataProvider dataProvider = dataProviderFunction.get();

        TitleMetaData tmd = TitleMetaData.parseTMD(dataProvider.getRawTMD().orElseThrow(() -> new FileNotFoundException("No TMD data found")));

        if (config.isNoDecryption()) {
            NUSTitle result = NUSTitle.create(tmd, dataProcessorFunction.apply(dataProvider, Optional.empty(), Optional.empty()), Optional.empty(),
                    Optional.empty());
            return result;
        }

        Optional<Ticket> ticket = Optional.empty();
        Optional<ContentDecryptor> decryption = Optional.empty();
        Optional<ContentEncryptor> encryption = Optional.empty();
        if (config.isTicketNeeded()) {
            Ticket ticketT = config.getTicket();
            if (ticketT == null) {
                Optional<byte[]> ticketOpt = dataProvider.getRawTicket();
                if (ticketOpt.isPresent()) {
                    ticketT = Ticket.parseTicket(ticketOpt.get(), config.getCommonKey());
                }
            }
            if (ticketT == null) {
                throw new ParseException("Failed to get ticket data", 0);
            }

            ticket = Optional.of(ticketT);

            decryption = Optional.of(new NUSDecryption(ticketT));
            try {
                encryption = Optional.of(new NUSEncryption(ticketT));
            } catch (NoSuchProviderException e) {
                throw new IOException(e);
            }
        }

        NUSDataProcessor dpp = dataProcessorFunction.apply(dataProvider, decryption, encryption);

        // If we have just content, we don't have a FST.
        if (tmd.getAllContents().size() == 1) {
            // The only way to check if the key is right, is by trying to decrypt the whole thing.
            NUSTitle result = NUSTitle.create(tmd, dpp, ticket, Optional.empty());

            FSTDataProvider dp = new FSTDataProviderNUSTitle(result);
            for (NodeEntry child : dp.getRoot().getFileChildren()) {
                if (!child.isLink()) {
                    dp.readFile((FileEntry) child);
                }
            }

            return result;
        }
        // If we have more than one content, the index 0 is the FST.
        Content fstContent = tmd.getContentByIndex(0);

        byte[] fstBytes = dpp.readPlainDecryptedContent(fstContent, true);
        FST fst = FST.parseData(fstBytes);

        // The dataprovider may need the FST to calculate the offset of a content
        // on the partition.
        dataProvider.setFST(fst);

        return NUSTitle.create(tmd, dpp, ticket, Optional.of(fst));
    }

}
