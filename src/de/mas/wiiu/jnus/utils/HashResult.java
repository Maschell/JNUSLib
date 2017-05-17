package de.mas.wiiu.jnus.utils;

import lombok.Data;

@Data
public class HashResult {
    private final byte[] SHA1;
    private final byte[] MD5;
    private final byte[] CRC32;

    @Override
    public String toString() {
        return "HashResult [SHA1=" + Utils.ByteArrayToString(SHA1) + ", MD5=" + Utils.ByteArrayToString(MD5) + ", CRC32=" + Utils.ByteArrayToString(CRC32)
                + "]";
    }
}
