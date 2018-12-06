/****************************************************************************
 * Copyright (C) 2017-2018 Maschell
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

public class Settings {
    public static String URL_BASE = "http://ccs.cdn.c.shop.nintendowifi.net/ccs/download";
    public static final int LATEST_TMD_VERSION = 0;
    public static final String TMD_FILENAME = "title.tmd";
    public static final String TICKET_FILENAME = "title.tik";
    public static final String CERT_FILENAME = "title.cert";

    public static final String ENCRYPTED_CONTENT_EXTENTION = ".app";
    public static final String DECRYPTED_CONTENT_EXTENTION = ".dec";
    public static final String WUD_KEY_FILENAME = "game.key";
    public static final String WOOMY_METADATA_FILENAME = "metadata.xml";
    public static final String H3_EXTENTION = ".h3";
    public static final String USER_AGENT = "Mozilla/5.0 (Nintendo WiiU) AppleWebKit/536.28 (KHTML, like Gecko) NX/3.0.3.12.12 NintendoBrowser/3.0.0.9561.US";

    public static byte[] commonKey = new byte[0x10];
    public static int WIIU_DECRYPTED_AREA_OFFSET = 0x18000;
}
