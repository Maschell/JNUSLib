package de.mas.wiiu.jnus.entities.FST.header;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import de.mas.wiiu.jnus.utils.ByteUtils;
import de.mas.wiiu.jnus.utils.blocksize.SectionBlockSize;
import lombok.Data;

@Data
public class Header {
    public static String MAGIC = "FST";
    public static int LENGTH = 32;
    private short FSTVersion;
    private SectionBlockSize blockSize;
    private long numberOfSections;
    private short hashDisabled;

    public static Header parseData(byte[] data, long offset) throws IOException {
        Header header = new Header();

        byte[] strRaw = Arrays.copyOfRange(data, 0, 3);
        String compareMagic = new String(strRaw, Charset.forName("ISO-8859-1"));
        if (!MAGIC.equals(compareMagic)) {
            throw new IOException("FST Header magic was wrong");
        }
        header.FSTVersion = ByteUtils.getByteFromBytes(data, 3);
        header.blockSize = new SectionBlockSize(ByteUtils.getUnsingedIntFromBytes(data, 4) & 0xFFFFFFFF);
        header.numberOfSections = ByteUtils.getUnsingedIntFromBytes(data, 8) & 0xFFFFFFFF;
        header.hashDisabled = ByteUtils.getByteFromBytes(data, 12);

        return header;
    }

    public byte[] getAsBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(LENGTH);
        buffer.put(MAGIC.getBytes());
        buffer.putInt(4, (int) blockSize.getBlockSize());
        buffer.putInt(8, (int) numberOfSections);
        buffer.put(12, (byte) hashDisabled);
        return buffer.array();
    }
}
