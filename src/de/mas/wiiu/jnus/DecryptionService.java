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
package de.mas.wiiu.jnus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import de.mas.wiiu.jnus.entities.fst.FSTEntry;
import de.mas.wiiu.jnus.interfaces.FSTDataProvider;
import de.mas.wiiu.jnus.utils.CheckSumWrongException;
import de.mas.wiiu.jnus.utils.FSTUtils;
import de.mas.wiiu.jnus.utils.FileUtils;
import de.mas.wiiu.jnus.utils.Utils;
import lombok.val;
import lombok.extern.java.Log;

@Log
public final class DecryptionService {

    private final FSTDataProvider dataProvider;

    private boolean parallelizable = false;

    public static DecryptionService getInstance(FSTDataProvider dataProvider) {
        return new DecryptionService(dataProvider);
    }

    private DecryptionService(FSTDataProvider dataProvider) {
        // if (dataProvider instanceof Parallelizable) {
        // parallelizable = true;
        // }
        this.dataProvider = dataProvider;
    }

    public void decryptFSTEntryTo(boolean useFullPath, FSTEntry entry, String outputPath, boolean skipExistingFile) {
        try {
            decryptFSTEntryToAsync(useFullPath, entry, outputPath, skipExistingFile).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public CompletableFuture<Void> decryptFSTEntryToAsync(boolean useFullPath, FSTEntry entry, String outputPath, boolean skipExistingFile) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (entry.isNotInPackage()) {
                    return;
                }

                log.info("Decrypting " + entry.getFilename());

                String targetFilePath = new StringBuilder().append(outputPath).append("/").append(entry.getFilename()).toString();
                String fullPath = new StringBuilder().append(outputPath).toString();

                if (useFullPath) {
                    targetFilePath = new StringBuilder().append(outputPath).append(entry.getFullPath()).toString();
                    fullPath = new StringBuilder().append(outputPath).append(entry.getPath()).toString();
                    if (entry.isDir()) { // If the entry is a directory. Create it and return.
                        Utils.createDir(targetFilePath);
                        return;
                    }
                } else if (entry.isDir()) {
                    return;
                }

                if (!Utils.createDir(fullPath)) {
                    return;
                }

                File target = new File(targetFilePath);

                if (skipExistingFile) {
                    File targetFile = new File(targetFilePath);
                    if (targetFile.exists()) {
                        if (entry.isDir()) {
                            return;
                        }
                        if (targetFile.length() == entry.getFileSize()) {

                            log.info("File already exists: " + entry.getFilename());
                            return;

                        } else {
                            log.info("File already exists but the filesize doesn't match: " + entry.getFilename());
                        }
                    }
                }

                // to avoid having fragmented files.
                FileUtils.FileAsOutputStreamWrapper(new File(targetFilePath), entry.getFileSize(),
                        newOutputStream -> decryptFSTEntryToStream(entry, newOutputStream));
            } catch (Exception ex) {
                throw new CompletionException(ex);
            }
        });
    }

    public void decryptFSTEntryToStream(FSTEntry entry, OutputStream outputStream) throws IOException {
        dataProvider.readFileToStream(outputStream, entry);
    }

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // Decrypt FSTEntry to OutputStream
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    public void decryptFSTEntryTo(String entryFullPath, OutputStream outputStream) throws IOException, CheckSumWrongException {
        FSTEntry entry = FSTUtils.getFSTEntryByFullPath(dataProvider.getRoot(), entryFullPath)
                .orElseThrow(() -> new FileNotFoundException("File not found: " + entryFullPath));

        decryptFSTEntryToStream(entry, outputStream);
    }

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // Decrypt single FSTEntry to File
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    public void decryptFSTEntryTo(boolean fullPath, String entryFullPath, String outputFolder, boolean skipExistingFiles)
            throws IOException, CheckSumWrongException {

        FSTEntry entry = FSTUtils.getFSTEntryByFullPath(dataProvider.getRoot(), entryFullPath)
                .orElseThrow(() -> new FileNotFoundException("File not found: " + entryFullPath));

        decryptFSTEntryTo(fullPath, entry, outputFolder, skipExistingFiles);
    }

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // Decrypt list of FSTEntry to Files
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    public void decryptAllFSTEntriesTo(String outputFolder, boolean skipExisting) throws IOException, CheckSumWrongException {
        Utils.createDir(outputFolder + File.separator + "code");
        Utils.createDir(outputFolder + File.separator + "content");
        Utils.createDir(outputFolder + File.separator + "meta");
        decryptFSTEntriesTo(true, ".*", outputFolder, skipExisting);
    }

    public void decryptFSTEntriesTo(String regEx, String outputFolder, boolean skipExisting) throws IOException, CheckSumWrongException {
        decryptFSTEntriesTo(true, regEx, outputFolder, skipExisting);
    }

    public void decryptFSTEntriesTo(boolean fullPath, String regEx, String outputFolder, boolean skipExisting) throws IOException, CheckSumWrongException {
        decryptFSTEntryListTo(fullPath, FSTUtils.getFSTEntriesByRegEx(dataProvider.getRoot(), regEx), outputFolder, skipExisting);
    }

    public void decryptFSTEntryListTo(List<FSTEntry> list, String outputFolder, boolean skipExisting) throws IOException, CheckSumWrongException {
        decryptFSTEntryListTo(true, list, outputFolder, skipExisting);
    }

    public void decryptFSTEntryListTo(boolean fullPath, List<FSTEntry> list, String outputFolder, boolean skipExisting)
            throws IOException, CheckSumWrongException {
        if (parallelizable && Settings.ALLOW_PARALLELISATION) {
            try {
                decryptFSTEntryListToAsync(fullPath, list, outputFolder, skipExisting).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        } else {
            for (val entry : list) {
                decryptFSTEntryTo(fullPath, entry, outputFolder, skipExisting);
            }
        }
    }

    public CompletableFuture<Void> decryptFSTEntryListToAsync(boolean fullPath, List<FSTEntry> list, String outputFolder, boolean skipExisting)
            throws IOException, CheckSumWrongException {
        return CompletableFuture
                .allOf(list.stream().map(entry -> decryptFSTEntryToAsync(fullPath, entry, outputFolder, skipExisting)).toArray(CompletableFuture[]::new));
    }

}
