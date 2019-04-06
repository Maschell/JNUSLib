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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import de.mas.wiiu.jnus.NUSTitle;
import de.mas.wiiu.jnus.Settings;
import de.mas.wiiu.jnus.entities.content.Content;
import de.mas.wiiu.jnus.utils.FileUtils;
import de.mas.wiiu.jnus.utils.HashUtil;
import de.mas.wiiu.jnus.utils.StreamUtils;
import de.mas.wiiu.jnus.utils.Utils;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.java.Log;

@Log
/**
 * Service Methods for loading NUS/Content data from different sources
 * 
 * @author Maschell
 *
 */
public abstract class NUSDataProvider {
    @Getter private final NUSTitle NUSTitle;

    public NUSDataProvider(NUSTitle title) {
        this.NUSTitle = title;
    }

    /**
     * Saves the given content encrypted with his .h3 file in the given directory. The Target directory will be created if it's missing. If the content is not
     * hashed, no .h3 will be saved
     * 
     * @param content
     *            Content that should be saved
     * @param outputFolder
     *            Target directory where the files will be stored in.
     * @throws IOException
     */
    public void saveEncryptedContentWithH3Hash(@NonNull Content content, @NonNull String outputFolder) throws IOException {
        saveContentH3Hash(content, outputFolder);
        saveEncryptedContent(content, outputFolder);
    }

    /**
     * Saves the .h3 file of the given content into the given directory. The Target directory will be created if it's missing. If the content is not hashed, no
     * .h3 will be saved
     * 
     * @param content
     *            The content of which the h3 hashes should be saved
     * @param outputFolder
     * @return
     * @throws IOException
     */
    public void saveContentH3Hash(@NonNull Content content, @NonNull String outputFolder) throws IOException {
        if (!content.isHashed()) {
            return;
        }
        String h3Filename = String.format("%08X%s", content.getID(), Settings.H3_EXTENTION);
        File output = new File(outputFolder + File.separator + h3Filename);

        if (output.exists()) {
            if (Arrays.equals(content.getSHA2Hash(), HashUtil.hashSHA1(output))) {
                log.info(h3Filename + " already exists");
                return;
            } else {
                if (Arrays.equals(content.getSHA2Hash(), Arrays.copyOf(HashUtil.hashSHA256(output), 20))) { // 0005000c1f941200 used sha256 instead of SHA1
                    log.info(h3Filename + " already exists");
                    return;
                }
                log.warning(h3Filename + " already exists but hash is differrent than expected.");
            }
        }

        byte[] hash = getContentH3Hash(content);
        if (hash == null || hash.length == 0) {
            return;
        }

        log.warning("Saving " + h3Filename + " ");

        FileUtils.saveByteArrayToFile(output, hash);

    }

    /**
     * Saves the given content encrypted in the given directory. The Target directory will be created if it's missing. If the content is not encrypted at all,
     * it will be just saved anyway.
     * 
     * @param content
     *            Content that should be saved
     * @param outputFolder
     *            Target directory where the files will be stored in.
     * @return
     * @throws IOException
     */
    public void saveEncryptedContent(@NonNull Content content, @NonNull String outputFolder) throws IOException {
        int maxTries = 3;
        int i = 0;
        while (i < maxTries) {
            File output = new File(outputFolder + File.separator + content.getFilename());
            if (output.exists()) {
                if (output.length() == content.getEncryptedFileSizeAligned()) {
                    log.info(content.getFilename() + "Encrypted content alreadys exists, skipped");
                    return;
                } else {
                    log.warning(content.getFilename() + " Encrypted content alreadys exists, but the length is not as expected. Saving it again. "
                            + output.length() + " " + content.getEncryptedFileSizeAligned() + " Difference: "
                            + (output.length() - content.getEncryptedFileSizeAligned()));
                }
            }

            Utils.createDir(outputFolder);
            InputStream inputStream = getInputStreamFromContent(content, 0);
            if (inputStream == null) {
                log.info(content.getFilename() + " Couldn't save encrypted content. Input stream was null");
                return;
            }
            log.warning("loading " + content.getFilename());
            FileUtils.saveInputStreamToFile(output, inputStream, content.getEncryptedFileSizeAligned());

            File outputNow = new File(outputFolder + File.separator + content.getFilename());
            if (outputNow.exists()) {
                if (outputNow.length() != content.getEncryptedFileSizeAligned()) {
                    log.warning(content.getFilename() + " Encrypted content length is not as expected. Saving it again. Loaded: " + outputNow.length()
                            + " Expected: " + content.getEncryptedFileSizeAligned() + " Difference: "
                            + (outputNow.length() - content.getEncryptedFileSizeAligned()));
                    i++;
                    continue;
                } else {
                    break;
                }
            }
        }

    }

    public byte[] getChunkFromContent(Content content, long offset, int size) throws IOException {
        return StreamUtils.getBytesFromStream(getInputStreamFromContent(content, offset), size);
    }

    /**
     * 
     * @param content
     * @param offset
     * @return
     * @throws IOException
     */
    public abstract InputStream getInputStreamFromContent(Content content, long offset) throws IOException;

    // TODO: JavaDocs
    public abstract byte[] getContentH3Hash(Content content) throws IOException;

    public abstract byte[] getRawTMD() throws IOException;

    public abstract byte[] getRawTicket() throws IOException;

    public abstract byte[] getRawCert() throws IOException;

    public abstract void cleanup() throws IOException;

}
