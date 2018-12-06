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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import de.mas.wiiu.jnus.entities.content.Content;
import de.mas.wiiu.jnus.implementations.NUSDataProvider;
import de.mas.wiiu.jnus.utils.FileUtils;
import de.mas.wiiu.jnus.utils.Parallelizable;
import de.mas.wiiu.jnus.utils.Utils;
import lombok.Getter;
import lombok.extern.java.Log;

@Log
public final class ExtractionService {
    private static Map<NUSTitle, ExtractionService> instances = new HashMap<>();

    @Getter private final NUSTitle NUSTitle;

    private boolean parallelizable = false;

    public static ExtractionService getInstance(NUSTitle nustitle) {
        if (!instances.containsKey(nustitle)) {
            instances.put(nustitle, new ExtractionService(nustitle));
        }
        return instances.get(nustitle);
    }

    private ExtractionService(NUSTitle nustitle) {
        if (nustitle.getDataProvider() instanceof Parallelizable) {
            parallelizable = true;
        }
        this.NUSTitle = nustitle;
    }

    private NUSDataProvider getDataProvider() {
        return getNUSTitle().getDataProvider();
    }

    public void extractAllEncrpytedContentFileHashes(String outputFolder) throws IOException {
        extractEncryptedContentHashesTo(new ArrayList<Content>(getNUSTitle().getTMD().getAllContents().values()), outputFolder);
    }

    public void extractEncryptedContentHashesTo(List<Content> list, String outputFolder) throws IOException {
        Utils.createDir(outputFolder);
        NUSDataProvider dataProvider = getDataProvider();
        for (Content c : list) {
            dataProvider.saveContentH3Hash(c, outputFolder);
        }
    }

    public void extractAllEncryptedContentFiles(String outputFolder) throws IOException {
        extractAllEncryptedContentFilesWithHashesTo(outputFolder);
    }

    public void extractAllEncryptedContentFilesWithoutHashesTo(String outputFolder) throws IOException {
        extractEncryptedContentFilesTo(new ArrayList<Content>(getNUSTitle().getTMD().getAllContents().values()), outputFolder, false);
    }

    public void extractAllEncryptedContentFilesWithHashesTo(String outputFolder) throws IOException {
        extractEncryptedContentFilesTo(new ArrayList<Content>(getNUSTitle().getTMD().getAllContents().values()), outputFolder, true);
    }

    public void extractEncryptedContentTo(Content content, String outputFolder, boolean withHashes) throws IOException {
        NUSDataProvider dataProvider = getDataProvider();
        if (withHashes) {
            dataProvider.saveEncryptedContentWithH3Hash(content, outputFolder);
        } else {
            dataProvider.saveEncryptedContent(content, outputFolder);
        }
    }

    public void extractEncryptedContentFilesTo(List<Content> list, String outputFolder, boolean withHashes) throws IOException {
        Utils.createDir(outputFolder);
        if (parallelizable && Settings.ALLOW_PARALLELISATION) {
            try {
                CompletableFuture.allOf(list.stream().map((Content c) -> CompletableFuture.runAsync(() -> {
                    try {
                        extractEncryptedContentTo(c, outputFolder, withHashes);
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                })).toArray(CompletableFuture[]::new)).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        } else {
            for (Content c : list) {
                extractEncryptedContentTo(c, outputFolder, withHashes);
            }
        }
    }

    public void extractTMDTo(String output) throws IOException {
        Utils.createDir(output);

        byte[] rawTMD = getDataProvider().getRawTMD();

        if (rawTMD == null || rawTMD.length == 0) {
            log.info("Couldn't write TMD: No TMD loaded");
            return;
        }
        String tmd_path = output + File.separator + Settings.TMD_FILENAME;
        log.info("Extracting TMD to: " + tmd_path);
        FileUtils.saveByteArrayToFile(tmd_path, rawTMD);
    }

    public boolean extractTicketTo(String output) throws IOException {
        Utils.createDir(output);

        byte[] rawTicket = getDataProvider().getRawTicket();

        if (rawTicket == null || rawTicket.length == 0) {
            log.info("Couldn't write Ticket: No Ticket loaded");
            return false;
        }
        String ticket_path = output + File.separator + Settings.TICKET_FILENAME;
        log.info("Extracting Ticket to: " + ticket_path);
        return FileUtils.saveByteArrayToFile(ticket_path, rawTicket);
    }

    public void extractCertTo(String output) throws IOException {
        Utils.createDir(output);

        byte[] rawCert = getDataProvider().getRawCert();

        if (rawCert == null || rawCert.length == 0) {
            log.info("Couldn't write Cert: No Cert loaded");
            return;
        }
        String cert_path = output + File.separator + Settings.CERT_FILENAME;
        log.info("Extracting Cert to: " + cert_path);
        FileUtils.saveByteArrayToFile(cert_path, rawCert);
    }

    public void extractAll(String outputFolder) throws IOException {
        Utils.createDir(outputFolder);

        extractAllEncryptedContentFilesWithHashesTo(outputFolder);
        extractCertTo(outputFolder);
        extractTMDTo(outputFolder);
        extractTicketTo(outputFolder);

    }

}
