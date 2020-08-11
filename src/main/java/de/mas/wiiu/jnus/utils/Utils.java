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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import de.mas.wiiu.jnus.Settings;
import lombok.extern.java.Log;

@Log
public final class Utils {
    private Utils() {
        // Utility class
    }

    public static long align(long numToRound, int multiple) {
        if ((multiple > 0) && ((multiple & (multiple - 1)) == 0)) {
            return alignPower2(numToRound, multiple);
        } else {
            return alignGeneric(numToRound, multiple);
        }
    }

    private static long alignGeneric(long numToRound, int multiple) {
        int isPositive = 0;
        if (numToRound >= 0) {
            isPositive = 1;
        }
        return ((numToRound + isPositive * (multiple - 1)) / multiple) * multiple;
    }

    private static long alignPower2(long numToRound, int multiple) {
        if (!((multiple > 0) && ((multiple & (multiple - 1)) == 0))) return 0L;
        return (numToRound + (multiple - 1)) & ~(multiple - 1);
    }

    public static String ByteArrayToString(byte[] ba) {
        if (ba == null) return null;
        StringBuilder hex = new StringBuilder(ba.length * 2);
        for (byte b : ba) {
            hex.append(String.format("%02X", b));
        }
        return hex.toString();
    }

    public static byte[] StringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static boolean createDir(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        File pathFile = new File(path);
        if (!pathFile.exists()) {
            boolean success = new File(path).mkdirs();
            if (!success) {
                log.fine("Creating directory \"" + path + "\" failed.");
                return false;
            }
        } else if (!pathFile.isDirectory()) {
            log.info("\"" + path + "\" already exists but is no directoy");
            return false;
        }
        return true;
    }

    public static void deleteDir(File path) {
        if (path == null || !path.exists()) return;
        for (File file : path.listFiles()) {
            if (file.isDirectory()) deleteDir(file);
            file.delete();
        }
        path.delete();
    }

    public static String replaceStringInStringEscaped(String input, String replace, String replaceWith) {
        return input.replaceAll(Pattern.quote(replace), Matcher.quoteReplacement(replaceWith));
    }

    public static boolean checkXML(InputSource in) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(true);

            DocumentBuilder builder = factory.newDocumentBuilder();

            builder.setErrorHandler(new ErrorHandler() {

                @Override
                public void warning(SAXParseException exception) throws SAXException {
                    throw exception;
                }

                @Override
                public void error(SAXParseException exception) throws SAXException {
                    throw exception;

                }

                @Override
                public void fatalError(SAXParseException exception) throws SAXException {
                    throw exception;
                }

            });

            builder.parse(in);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean checkXML(InputStream in) {
        return checkXML(new InputSource(in));
    }

    public static boolean checkXML(byte[] data) {
        return checkXML(new ByteArrayInputStream(data));
    }

    public static boolean checkXML(File file) {
        return checkXML(new InputSource(file.getAbsolutePath()));
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    public static long StringToLong(String s) {
        try {
            BigInteger bi = new BigInteger(s, 16);
            return bi.longValue();
        } catch (NumberFormatException e) {
            System.err.println("Invalid Title ID");
            return 0L;
        }
    }

    public static void setGlobalLogLevel(Level level) {
        Arrays.stream(LogManager.getLogManager().getLogger("").getHandlers()).forEach(h -> h.setLevel(level));
    }

    public static boolean checkFileExists(String path) {
        return new File(path).exists();
    }

    /**
     * Pings a HTTP URL. This effectively sends a HEAD request and returns <code>true</code> if the response code is in the 200-399 range.
     * 
     * @param url
     *            The HTTP URL to be pinged.
     * @param timeout
     *            The timeout in millis for both the connection timeout and the response read timeout. Note that the total timeout is effectively two times the
     *            given timeout.
     * @return <code>true</code> if the given HTTP URL has returned response code 200-399 on a HEAD request within the given timeout, otherwise
     *         <code>false</code>.
     */
    public static boolean pingURL(String url, int timeout) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestProperty("User-Agent", Settings.USER_AGENT);
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            int responseCode = connection.getResponseCode();
            return (200 <= responseCode && responseCode <= 399);
        } catch (IOException exception) {
            return false;
        }
    }

    public static Long getLastModifiedURL(String url, int timeout) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", Settings.USER_AGENT);
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);

        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            InputStream inputStream = connection.getInputStream();
            byte[] buffer = new byte[0x10];
            inputStream.read(buffer);
            inputStream.close();
        } else {
            return null;
        }

        Long dateTime = connection.getLastModified();

        if (200 <= responseCode && responseCode <= 399) {
            return dateTime;
        }

        return null;
    }

    public static Long getLastModifiedURL(HttpURLConnection connectionForURL, int timeout) throws IOException {
        HttpURLConnection connection = connectionForURL;
        connection.setRequestProperty("User-Agent", Settings.USER_AGENT);
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);

        int responseCode = connection.getResponseCode();

        if (responseCode == HttpsURLConnection.HTTP_OK) {
            InputStream inputStream = connection.getInputStream();
            byte[] buffer = new byte[0x10];
            inputStream.read(buffer);
            inputStream.close();
        } else {
            return null;
        }

        Long dateTime = connection.getLastModified();

        if (200 <= responseCode && responseCode <= 399) {
            return dateTime;
        }

        return null;
    }

}
