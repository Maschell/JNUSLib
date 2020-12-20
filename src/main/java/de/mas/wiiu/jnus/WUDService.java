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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import de.mas.wiiu.jnus.implementations.wud.WUDImage;
import de.mas.wiiu.jnus.implementations.wud.WUDImageCompressedInfo;
import de.mas.wiiu.jnus.utils.ByteArrayBuffer;
import de.mas.wiiu.jnus.utils.ByteArrayWrapper;
import de.mas.wiiu.jnus.utils.HashResult;
import de.mas.wiiu.jnus.utils.HashUtil;
import de.mas.wiiu.jnus.utils.StreamUtils;
import de.mas.wiiu.jnus.utils.Utils;
import lombok.extern.java.Log;

@Log
public final class WUDService {
    private WUDService() {
        // Just an utility class
    }

    public static Optional<File> compressWUDToWUX(WUDImage image, String outputFolder) throws IOException {
        return compressWUDToWUX(image, outputFolder, "game.wux", false, Optional.empty());
    }

    public static Optional<File> compressWUDToWUX(WUDImage image, String outputFolder, boolean overwrite) throws IOException {
        return compressWUDToWUX(image, outputFolder, "game.wux", overwrite, Optional.empty());
    }

    public static Optional<File> compressWUDToWUX(WUDImage image, String outputFolder, String filename, boolean overwrite, Optional<Long> inputFileSize) throws IOException {
        if (image.isCompressed()) {
            log.info("Given image is already compressed");
            //return Optional.empty();
        }

        if (image.getWUDFileSize() != WUDImage.WUD_FILESIZE) {
            log.info("Given WUD has not the expected filesize");
            //return Optional.empty();
        }

        String usedOutputFolder = outputFolder;
        if (usedOutputFolder == null) usedOutputFolder = "";
        Utils.createDir(usedOutputFolder);

        String filePath;
        if (usedOutputFolder.isEmpty()) {
            filePath = filename;
        } else {
            filePath = usedOutputFolder + File.separator + filename;
        }

        File outputFile = new File(filePath);

        if (outputFile.exists() && !overwrite) {
            log.info("Couldn't compress wud, target file already exists (" + outputFile.getAbsolutePath() + ")");
            return Optional.empty();
        }
        
        long toReadFilesize = image.getWUDFileSize();
        if(inputFileSize.isPresent()) {
            toReadFilesize = inputFileSize.get();         
        }

        log.info("Writing compressed file to: " + outputFile.getAbsolutePath());
        RandomAccessFile fileOutput = new RandomAccessFile(outputFile, "rw");
        try {

            WUDImageCompressedInfo info = new WUDImageCompressedInfo(WUDImageCompressedInfo.SECTOR_SIZE, 0, toReadFilesize);

            byte[] header = info.getHeaderAsBytes();
            log.info("Writing header + " + header.length);
            fileOutput.write(header);

            int sectorTableEntryCount = (int) ((toReadFilesize + WUDImageCompressedInfo.SECTOR_SIZE - 1) / (long) WUDImageCompressedInfo.SECTOR_SIZE);

            long sectorTableStart = fileOutput.getFilePointer();
            long sectorTableEnd = Utils.align(sectorTableStart + (sectorTableEntryCount * 0x04), WUDImageCompressedInfo.SECTOR_SIZE);
            byte[] sectorTablePlaceHolder = new byte[(int) (sectorTableEnd - sectorTableStart)];

            fileOutput.write(sectorTablePlaceHolder);

            Map<ByteArrayWrapper, Integer> sectorHashes = new HashMap<>();
            Map<Integer, Integer> sectorMapping = new TreeMap<>();

            InputStream in = image.getWUDDiscReader().readEncryptedToStream(0, toReadFilesize);

            int bufferSize = WUDImageCompressedInfo.SECTOR_SIZE;
            byte[] blockBuffer = new byte[bufferSize];
            ByteArrayBuffer overflow = new ByteArrayBuffer(bufferSize);

            long written = 0;
            int curSector = 0;
            int realSector = 0;

            log.info("Writing sectors");
            Integer oldOffset = null;
            do {
                int read = StreamUtils.getChunkFromStream(in, blockBuffer, overflow, bufferSize);
                ByteArrayWrapper hash;
                try {
                    hash = new ByteArrayWrapper(HashUtil.hashSHA1(blockBuffer));
                } catch (NoSuchAlgorithmException e1) {
                    throw new IOException(e1);
                }

                if ((oldOffset = sectorHashes.get(hash)) == null) {
                    sectorMapping.put(curSector, realSector);
                    sectorHashes.put(hash, realSector);
                    fileOutput.write(blockBuffer);
                    realSector++;
                } else {
                    sectorMapping.put(curSector, oldOffset);
                    oldOffset = null;
                }

                written += read;
                curSector++;
                if (curSector % 10 == 0) {
                    double readMB = written / 1024.0 / 1024.0;
                    double writtenMB = ((long) realSector * (long) bufferSize) / 1024.0 / 1024.0;
                    double percent = ((double) written / toReadFilesize) * 100;
                    double ratio = 1 / (writtenMB / readMB);
                    System.out.print(String.format(Locale.ROOT, "\rCompressing into .wux | Progress %.2f%% | Ratio: 1:%.2f | Read: %.2fMB | Written: %.2fMB\t",
                            percent, ratio, readMB, writtenMB));
                }
            } while (written < toReadFilesize);
            System.out.println();
            log.info("Sectors compressed.");
            log.info("Writing sector table");
            fileOutput.seek(sectorTableStart);
            ByteBuffer buffer = ByteBuffer.allocate(sectorTablePlaceHolder.length);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            for (Entry<Integer, Integer> e : sectorMapping.entrySet()) {
                buffer.putInt(e.getValue());
            }

            fileOutput.write(buffer.array());
        } finally {
            fileOutput.close();
        }

        return Optional.of(outputFile);
    }

    public static boolean compareWUDImage(WUDImage firstImage, WUDImage secondImage) throws IOException {
        if (firstImage.getWUDFileSize() != secondImage.getWUDFileSize()) {
            log.warning("Filesize is different");
            return false;
        }
        InputStream in1 = firstImage.getWUDDiscReader().readEncryptedToStream(0, WUDImage.WUD_FILESIZE);
        InputStream in2 = secondImage.getWUDDiscReader().readEncryptedToStream(0, WUDImage.WUD_FILESIZE);

        boolean result = true;
        int bufferSize = 1024 * 1024 + 19;
        long totalread = 0;
        byte[] blockBuffer1 = new byte[bufferSize];
        byte[] blockBuffer2 = new byte[bufferSize];
        ByteArrayBuffer overflow1 = new ByteArrayBuffer(bufferSize);
        ByteArrayBuffer overflow2 = new ByteArrayBuffer(bufferSize);
        long curSector = 0;
        do {
            int read1 = StreamUtils.getChunkFromStream(in1, blockBuffer1, overflow1, bufferSize);
            int read2 = StreamUtils.getChunkFromStream(in2, blockBuffer2, overflow2, bufferSize);
            if (read1 != read2) {
                log.warning("Verification error");
                result = false;
                break;
            }

            if (!Arrays.equals(blockBuffer1, blockBuffer2)) {
                log.warning("Verification error");
                result = false;
                break;
            }

            totalread += read1;

            curSector++;
            if (curSector % 1 == 0) {
                double readMB = totalread / 1024.0 / 1024.0;
                double percent = ((double) totalread / WUDImage.WUD_FILESIZE) * 100;
                System.out.print(String.format("\rVerification: %.2fMB done (%.2f%%)", readMB, percent));
            }
        } while (totalread < WUDImage.WUD_FILESIZE);
        System.out.println();
        log.info("Verfication done!");
        in1.close();
        in2.close();

        return result;
    }

    public static Optional<File> decompressWUX(WUDImage image, String outputFolder) throws IOException {
        return decompressWUX(image, outputFolder, "game.wud", false);
    }

    public static Optional<File> decompressWUX(WUDImage image, String outputFolder, boolean overwrite) throws IOException {
        return decompressWUX(image, outputFolder, "game.wud", overwrite);
    }

    public static Optional<File> decompressWUX(WUDImage image, String outputFolder, String filename, boolean overwrite) throws IOException {
        if (!image.isCompressed()) {
            log.info("Given image is already decompressed (a wud file)");
            return Optional.empty();
        }

        if (image.getWUDFileSize() != WUDImage.WUD_FILESIZE) {
            log.info("Given WUX has not the expected filesize");
            //return Optional.empty();
        }

        String usedOutputFolder = outputFolder;
        if (usedOutputFolder == null) usedOutputFolder = "";
        Utils.createDir(usedOutputFolder);

        String filePath;
        if (usedOutputFolder.isEmpty()) {
            filePath = filename;
        } else {
            filePath = usedOutputFolder + File.separator + filename;
        }

        File outputFile = new File(filePath);

        if (outputFile.exists() && !overwrite) {
            log.info("Couldn't decompress wux, target file already exists (" + outputFile.getAbsolutePath() + ")");
            return Optional.empty();
        }
        
        long toReadFilesize = image.getWUDFileSize() ;

        log.info("Writing decompressed file to: " + outputFile.getAbsolutePath());
        InputStream in = image.getWUDDiscReader().readEncryptedToStream(0, toReadFilesize);
        OutputStream out = new FileOutputStream(outputFile);

        int bufferSize = 1024 * 1024;
        long totalread = 0;
        byte[] blockBuffer = new byte[bufferSize];
        ByteArrayBuffer overflow = new ByteArrayBuffer(bufferSize);
        long curSector = 0;
        do {
            int read = StreamUtils.getChunkFromStream(in, blockBuffer, overflow, bufferSize);

            if (read == bufferSize) {
                out.write(blockBuffer);
            } else if (read > 0) {
                out.write(Arrays.copyOfRange(blockBuffer, 0, read));
            }

            totalread += read;

            curSector++;
            if (curSector % 1 == 0) {
                double readMB = totalread / 1024.0 / 1024.0;
                double percent = ((double) totalread / toReadFilesize) * 100;
                System.out.print(String.format("\rDecompressing: %.2fMB done (%.2f%%)", readMB, percent));
            }
        } while (totalread < toReadFilesize);
        System.out.println();
        log.info("Decompressing done!");
        in.close();
        out.close();

        return Optional.of(outputFile);
    }

    public static HashResult hashWUDImage(WUDImage image) throws IOException {
        if (image == null) {
            log.info("Failed to calculate the hash of the given image: input was null.");
            throw new IOException("Failed to calculate the hash of the given image: input was null.");
        }

        if (image.isCompressed()) {
            log.info("The input file is compressed. The calculated hash is the hash of the corresponding .wud file, not this .wux!");
        } else if (image.isSplitted()) {
            log.info("The input file is splitted. The calculated hash is the hash of the corresponding .wud file, not this splitted .wud");
        }

        InputStream in = image.getWUDDiscReader().readEncryptedToStream(0, WUDImage.WUD_FILESIZE);

        int bufferSize = 1024 * 1024 * 10;
        long totalread = 0;
        byte[] blockBuffer1 = new byte[bufferSize];
        ByteArrayBuffer overflow1 = new ByteArrayBuffer(bufferSize);
        long curSector = 0;

        MessageDigest sha1 = null;
        MessageDigest md5 = null;
        Checksum checksumEngine = new CRC32();

        try {
            sha1 = MessageDigest.getInstance("SHA1");
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        do {
            int read1 = StreamUtils.getChunkFromStream(in, blockBuffer1, overflow1, bufferSize);
            sha1.update(blockBuffer1, 0, read1);
            md5.update(blockBuffer1, 0, read1);
            checksumEngine.update(blockBuffer1, 0, read1);

            totalread += read1;

            curSector++;
            if (curSector % 10 == 0) {
                double readMB = totalread / 1024.0 / 1024.0;
                double percent = ((double) totalread / WUDImage.WUD_FILESIZE) * 100;
                System.out.print(String.format("\rHashing: %.2fMB done (%.2f%%)", readMB, percent));
            }
        } while (totalread < WUDImage.WUD_FILESIZE);
        double readMB = totalread / 1024.0 / 1024.0;
        double percent = ((double) totalread / WUDImage.WUD_FILESIZE) * 100;

        log.info(String.format("\rHashing: %.2fMB done (%.2f%%)", readMB, percent));

        HashResult result = new HashResult(sha1.digest(), md5.digest(), Utils.StringToByteArray(Long.toHexString(checksumEngine.getValue())));

        in.close();

        return result;
    }
}
