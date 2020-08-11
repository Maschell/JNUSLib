package de.mas.wiiu.jnus.implementations.wud.header;

import java.io.IOException;
import java.nio.ByteBuffer;

import de.mas.wiiu.jnus.implementations.wud.reader.WUDDiscReader;
import de.mas.wiiu.jnus.utils.Utils;
import lombok.Data;

@Data
public class WiiUManufactorDiscID {
    public final static long LENGTH = 65536;
    private final byte[] data;

    static WiiUManufactorDiscID parseData(WUDDiscReader reader, long offset) throws IOException {
        byte[] data = reader.readEncryptedToByteArray(offset, 0, LENGTH);
        if (data.length != LENGTH) {
            throw new IOException("Failed to read ManufactorDiscID");
        }

        return new WiiUManufactorDiscID(data);
    }

    @Override
    public String toString() {
        return "WiiUManufactorDiscID [data=" + Utils.ByteArrayToString(data) + "]";
    }

    public byte[] getAsBytes() {
        return ByteBuffer.wrap(data).array();
    }

}
