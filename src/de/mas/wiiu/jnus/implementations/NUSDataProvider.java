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

import de.mas.wiiu.jnus.NUSTitle;
import de.mas.wiiu.jnus.Settings;
import de.mas.wiiu.jnus.entities.content.Content;
import de.mas.wiiu.jnus.utils.FileUtils;
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
     * @throws IOException
     */
    public void saveContentH3Hash(@NonNull Content content, @NonNull String outputFolder) throws IOException {
        if (!content.isHashed()) {
            return;
        }
        byte[] hash = getContentH3Hash(content);
        if (hash == null || hash.length == 0) {
            return;
        }
        String h3Filename = String.format("%08X%s", content.getID(), Settings.H3_EXTENTION);
        File output = new File(outputFolder + File.separator + h3Filename);
        if (output.exists() && output.length() == hash.length) {
            log.info(h3Filename + " already exists");
            return;
        }

        log.info("Saving " + h3Filename + " ");

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
     * @throws IOException
     */
    public void saveEncryptedContent(@NonNull Content content, @NonNull String outputFolder) throws IOException {
        Utils.createDir(outputFolder);
        InputStream inputStream = getInputStreamFromContent(content, 0);
        if (inputStream == null) {
            log.info("Couldn't save encrypted content. Input stream was null");
            return;
        }

        File output = new File(outputFolder + File.separator + content.getFilename());
        if (output.exists()) {
            if (output.length() == content.getEncryptedFileSizeAligned()) {
                log.info("Encrypted content alreadys exists, skipped");
                return;
            } else {
                log.info("Encrypted content alreadys exists, but the length is not as expected. Saving it again");
            }
        }
        FileUtils.saveInputStreamToFile(output, inputStream, content.getEncryptedFileSizeAligned());
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
