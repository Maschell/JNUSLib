package de.mas.wiiu.jnus.implementations.wud.content;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;

import de.mas.wiiu.jnus.implementations.wud.reader.WUDDiscReader;
import de.mas.wiiu.jnus.utils.ByteUtils;
import de.mas.wiiu.jnus.utils.Utils;
import de.mas.wiiu.jnus.utils.blocksize.DiscBlockSize;
import lombok.Data;

@Data
public class WiiUDiscContentsHeader {
    public static final int LENGTH = 2048;
    public static final int MAGIC = 0xCCA6E67B;

    private DiscBlockSize blockSize = new DiscBlockSize(32768);
    private int numberOfPartition = 0;
    private byte[] tocHash = new byte[20];

    public static WiiUDiscContentsHeader parseData(WUDDiscReader reader, Optional<byte[]> discKey, long offset) throws IOException {
        byte[] rawBytes = new byte[0];
        if (!discKey.isPresent()) {
            rawBytes = reader.readEncryptedToByteArray(offset, 0, LENGTH);
        } else {
            rawBytes = reader.readDecryptedToByteArray(offset, 0, LENGTH, discKey.get(), null, true);
        }

        if (rawBytes.length != LENGTH) {
            throw new IOException("Failed to read WiiUDiscContentsHeader");
        }

        ByteBuffer buffer = ByteBuffer.wrap(rawBytes);
        byte[] magicCompare = new byte[4];
        buffer.get(magicCompare);
        if (!Arrays.equals(magicCompare, ByteUtils.getBytesFromInt(MAGIC))) {
            throw new IOException("WiiUDiscContentsHeader MAGIC mismatch.");
        }

        WiiUDiscContentsHeader contentsHeader = new WiiUDiscContentsHeader();
        contentsHeader.setBlockSize(new DiscBlockSize(ByteUtils.getUnsingedIntFromBytes(rawBytes, 4) & 0xFFFFFFFF));
        contentsHeader.setTocHash(Arrays.copyOfRange(rawBytes, 8, 28));
        contentsHeader.setNumberOfPartition(ByteUtils.getIntFromBytes(rawBytes, 28));
        return contentsHeader;
    }

    @Override
    public String toString() {
        return "WiiUDiscContentsHeader [blockSize=" + blockSize + ", numberOfPartition=" + numberOfPartition + ", tocHash=" + Utils.ByteArrayToString(tocHash)
                + "]";
    }

    public byte[] getAsBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(LENGTH);
        buffer.put(ByteUtils.getBytesFromInt(MAGIC));
        buffer.putInt((int) blockSize.getBlockSize());
        buffer.put(tocHash);
        buffer.putInt(numberOfPartition);
        return buffer.array();
    }

}
