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
package de.mas.wiiu.jnus.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.extern.java.Log;

@Log
public final class HashUtil {
    private HashUtil() {
        // Utility class
    }

    public static byte[] hashSHA256(byte[] data) {
        MessageDigest sha256;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return new byte[0x20];
        }

        return sha256.digest(data);
    }

    public static byte[] hashSHA256(File file) {
        return hashSHA256(file, 0);
    }

    // TODO: testing
    public static byte[] hashSHA256(File file, int aligmnent) {
        byte[] hash = new byte[0x20];
        MessageDigest sha1 = null;
        try {
            InputStream in = new FileInputStream(file);
            sha1 = MessageDigest.getInstance("SHA-256");
            hash = hash(sha1, in, file.length(), 0x8000, aligmnent);
        } catch (NoSuchAlgorithmException | FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return hash;
    }

    public static byte[] hashSHA1(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA1");

        return sha1.digest(data);
    }

    public static byte[] hashSHA1(InputStream in, long length) throws NoSuchAlgorithmException, IOException {
        return hashSHA1(in, length, 0);
    }

    public static byte[] hashSHA1(InputStream in, long length, int aligmnent) throws IOException, NoSuchAlgorithmException {
        byte[] hash = new byte[0x14];
        MessageDigest sha1 = MessageDigest.getInstance("SHA1");
        hash = hash(sha1, in, length, 0x8000, aligmnent);

        return hash;
    }

    public static byte[] hashSHA1(File file) throws NoSuchAlgorithmException, IOException {
        return hashSHA1(file, 0);
    }

    public static byte[] hashSHA1(File file, int aligmnent) throws NoSuchAlgorithmException, IOException {
        InputStream in = new FileInputStream(file);
        return hashSHA1(in, file.length(), aligmnent);
    }

    public static byte[] hash(MessageDigest digest, InputStream in, long inputSize1, int bufferSize, int alignment) throws IOException {
        long target_size = alignment == 0 ? inputSize1 : Utils.align(inputSize1, alignment);
        long cur_position = 0;
        int inBlockBufferRead = 0;
        byte[] blockBuffer = new byte[bufferSize];
        ByteArrayBuffer overflow = new ByteArrayBuffer(bufferSize);
        try {
            do {
                inBlockBufferRead = StreamUtils.getChunkFromStream(in, blockBuffer, overflow, bufferSize);

                if (inBlockBufferRead <= 0) break;

                digest.update(blockBuffer, 0, inBlockBufferRead);
                cur_position += inBlockBufferRead;

            } while (cur_position < target_size);
            long missing_bytes = target_size - cur_position;
            if (missing_bytes > 0) {
                byte[] missing = new byte[(int) missing_bytes];
                digest.update(missing, 0, (int) missing_bytes);
            }
        } finally {
            in.close();
        }

        return digest.digest();
    }

    public static boolean compareHashFolder(File input1, File input2) throws NoSuchAlgorithmException, IOException {
        List<File> expectedFiles = getInputFilesForFolder(input1);
        List<File> givenFiles = getInputFilesForFolder(input2);
        String regexInput = input1.getAbsolutePath().toLowerCase();
        String regexOutput = input2.getAbsolutePath().toLowerCase();

        List<File> exptectedFileWithAdjustedPath = new ArrayList<>();
        for (File f : expectedFiles) {
            String newPath = Utils.replaceStringInStringEscaped(f.getAbsolutePath().toLowerCase(), regexInput, regexOutput).toLowerCase();
            exptectedFileWithAdjustedPath.add(new File(newPath.toLowerCase()));
        }

        if (!givenFiles.equals(exptectedFileWithAdjustedPath)) {
            log.info("Folder doesn't contain the same files.");
            List<File> additionalFileList = new ArrayList<>(givenFiles);
            List<File> missingFileList = new ArrayList<>(exptectedFileWithAdjustedPath);
            additionalFileList.removeAll(exptectedFileWithAdjustedPath);
            missingFileList.removeAll(givenFiles);
            if (!additionalFileList.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Additional files:\n");
                for (File f : additionalFileList) {
                    sb.append(f.toString() + "\n");
                }
                log.info(sb.toString());
            }

            if (!missingFileList.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Missing files:\n");
                for (File f : missingFileList) {
                    sb.append(f.toString() + "\n");
                }
                log.info(sb.toString());
            }
            return false;
        }

        boolean result = true;
        for (File f : expectedFiles) {
            File expected = f;
            String newPath = Utils.replaceStringInStringEscaped(f.getAbsolutePath().toLowerCase(), regexInput, regexOutput).toLowerCase();
            File given = new File(newPath);
            if (!compareHashFile(expected, given)) {
                result = false;
                break;
            }
        }
        return result;
    }

    private static boolean compareHashFile(File file1, File file2) throws NoSuchAlgorithmException, IOException {
        if (file1 == null || !file1.exists() || file2 == null || !file2.exists()) {
            return false;
        }
        if (file1.isDirectory() && file2.isDirectory()) {
            return true;
        } else if (file1.isDirectory() || file2.isDirectory()) {
            return false;
        }
        byte[] hash1 = HashUtil.hashSHA1(file1);
        byte[] hash2 = HashUtil.hashSHA1(file2);
        boolean result = Arrays.equals(hash1, hash2);
        if (!result) {
            log.warning("Hash doesn't match for " + file1.getAbsolutePath() + "(" + Utils.ByteArrayToString(hash1) + ") and " + file2.getAbsolutePath() + "("
                    + Utils.ByteArrayToString(hash2) + ")!");
        }
        return result;
    }

    private static List<File> getInputFilesForFolder(File input1) {
        if (input1 == null || !input1.exists()) return new ArrayList<>();
        List<File> result = new ArrayList<>();
        for (File f : input1.listFiles()) {
            if (f.isDirectory()) {
                result.add(f);
                result.addAll(getInputFilesForFolder(f));
            } else {
                result.add(f);
            }
        }
        return result;
    }

    public static void checkFileChunkHashes(byte[] hashes, byte[] h3Hashes, byte[] output, int block) throws CheckSumWrongException, NoSuchAlgorithmException {
        int H0_start = (block % 16) * 20;
        int H1_start = (16 + (block / 16) % 16) * 20;
        int H2_start = (32 + (block / 256) % 16) * 20;
        int H3_start = ((block / 4096) % 16) * 20;

        byte[] real_h0_hash = HashUtil.hashSHA1(output);
        byte[] expected_h0_hash = Arrays.copyOfRange(hashes, H0_start, H0_start + 20);

        if (!Arrays.equals(real_h0_hash, expected_h0_hash)) {
            throw new CheckSumWrongException("h0 checksumfail", real_h0_hash, expected_h0_hash);
        } else {
            log.finest("h0 checksum right!");
        }

        if ((block % 16) == 0) {
            byte[] expected_h1_hash = Arrays.copyOfRange(hashes, H1_start, H1_start + 20);
            byte[] real_h1_hash = HashUtil.hashSHA1(Arrays.copyOfRange(hashes, H0_start, H0_start + (16 * 20)));

            if (!Arrays.equals(expected_h1_hash, real_h1_hash)) {
                throw new CheckSumWrongException("h1 checksumfail", real_h1_hash, expected_h1_hash);
            } else {
                log.finest("h1 checksum right!");
            }
        }

        if ((block % 256) == 0) {
            byte[] expected_h2_hash = Arrays.copyOfRange(hashes, H2_start, H2_start + 20);
            byte[] real_h2_hash = HashUtil.hashSHA1(Arrays.copyOfRange(hashes, H1_start, H1_start + (16 * 20)));

            if (!Arrays.equals(expected_h2_hash, real_h2_hash)) {
                throw new CheckSumWrongException("h2 checksumfail", real_h2_hash, expected_h2_hash);
            } else {
                log.finest("h2 checksum right!");
            }
        }

        if (h3Hashes == null) {
            log.warning("didn't check the h3, its missing.");
            return;
        }
        if ((block % 4096) == 0) {
            byte[] expected_h3_hash = Arrays.copyOfRange(h3Hashes, H3_start, H3_start + 20);
            byte[] real_h3_hash = HashUtil.hashSHA1(Arrays.copyOfRange(hashes, H2_start, H2_start + (16 * 20)));

            if (!Arrays.equals(expected_h3_hash, real_h3_hash)) {
                throw new CheckSumWrongException("h3 checksumfail", real_h3_hash, expected_h3_hash);
            } else {
                log.finest("h3 checksum right!");
            }
        }

    }
}
