package de.mas.wiiu.jnus.implementations.wud.content.partitions.volumes;

import java.io.IOException;

import de.mas.wiiu.jnus.implementations.wud.reader.WUDDiscReader;
import de.mas.wiiu.jnus.utils.Utils;
import lombok.Data;

@Data
public class H3HashArray {
    public static int LENGTH = 20;
    private final byte[] H3HashArray;

    public static H3HashArray parseData(WUDDiscReader reader, long offset, long t_nLength) throws IOException {
        return new H3HashArray(reader.readEncryptedToByteArray(offset, 0, t_nLength));
    }

    @Override
    public String toString() {
        return "H3HashArray [H3HashArray=" + Utils.ByteArrayToString(H3HashArray) + "]";
    }

}
