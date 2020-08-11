package de.mas.wiiu.jnus.implementations.wud.header;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import de.mas.wiiu.jnus.implementations.wud.reader.WUDDiscReader;
import de.mas.wiiu.jnus.utils.ByteUtils;
import lombok.Data;

@Data
public class WiiUDiscID {
    public static final int MAGIC = 0xCC549EB9;
    public static final int LENGTH = 32768;

    private final byte majorVersion;
    private final byte minorVersion;
    private final String footprint;

    public static WiiUDiscID parseData(WUDDiscReader reader, long offset) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        if (reader.readEncryptedToStream(baos, offset, LENGTH) != LENGTH) {
            throw new IOException("Failed to read DiscId");
        }
        byte[] rawBytes = baos.toByteArray();
        ByteBuffer buffer = ByteBuffer.wrap(rawBytes);
        byte[] magicCompare = new byte[4];
        buffer.get(magicCompare);
        if (!Arrays.equals(magicCompare, ByteUtils.getBytesFromInt(MAGIC))) {
            throw new IOException("DiscId MAGIC mismatch.");
        }
        byte majorVersion = buffer.get();
        byte minorVersion = buffer.get();

        byte[] strRaw = Arrays.copyOfRange(rawBytes, 32, 32 + 64);
        for (int i = 0; i < strRaw.length; i++) {
            if (strRaw[i] == '\0') {
                strRaw = Arrays.copyOf(strRaw, i);
                break;
            }
        }

        String footprint = new String(strRaw, Charset.forName("ISO-8859-1"));

        return new WiiUDiscID(majorVersion, minorVersion, footprint);
    }

    public byte[] getAsBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(LENGTH);
        buffer.put(ByteUtils.getBytesFromInt(MAGIC));
        buffer.put(majorVersion);
        buffer.put(minorVersion);
        buffer.put(footprint.getBytes());
        return buffer.array();
    }
}
