package de.mas.jnus.lib;

public class Settings {
    public static String URL_BASE = "http://ccs.cdn.wup.shop.nintendo.net/ccs/download";
    public static final int LATEST_TMD_VERSION = 0;
    public static final String TMD_FILENAME = "title.tmd";
    public static final String TICKET_FILENAME = "title.tik";
    public static final String CERT_FILENAME = "title.cert";

    public static final String ENCRYPTED_CONTENT_EXTENTION = ".app";
    public static final String DECRYPTED_CONTENT_EXTENTION = ".dec";
    public static final String WUD_KEY_FILENAME = "game.key";
    public static final String WOOMY_METADATA_FILENAME = "metadata.xml";
    public static final String H3_EXTENTION = ".h3";

    public static byte[] commonKey = new byte[0x10];
    public static int WIIU_DECRYPTED_AREA_OFFSET = 0x18000;
}
