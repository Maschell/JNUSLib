package de.mas.wiiu.jnus.implementations.wud.content;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;

import de.mas.wiiu.jnus.implementations.wud.content.partitions.WiiUPartitions;
import de.mas.wiiu.jnus.implementations.wud.reader.WUDDiscReader;
import lombok.Data;

@Data
public class WiiUContentsInformation {

    public static final int LENGTH = 32768;
    private WiiUDiscContentsHeader discContentHeader = new WiiUDiscContentsHeader();
    private WiiUPartitions partitions = new WiiUPartitions();

    public static WiiUContentsInformation parseData(WUDDiscReader reader, Optional<byte[]> discKey, long offset) throws IOException {

        WiiUContentsInformation contentsInformation = new WiiUContentsInformation();
        long curOffset = offset;
        WiiUDiscContentsHeader discContentHeader = WiiUDiscContentsHeader.parseData(reader, discKey, curOffset);
        contentsInformation.setDiscContentHeader(discContentHeader);
        curOffset += WiiUDiscContentsHeader.LENGTH;
        contentsInformation.setPartitions(
                WiiUPartitions.parseData(reader, discKey, curOffset, discContentHeader.getNumberOfPartition(), discContentHeader.getBlockSize()));
        curOffset += WiiUPartitions.LENGTH;

        if (curOffset - offset != LENGTH) {
            throw new IOException("Length mismatch. " + (curOffset - offset) + " " + LENGTH);
        }

        return contentsInformation;
    }

    public byte[] getAsBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(LENGTH);
        buffer.put(discContentHeader.getAsBytes());
        buffer.put(partitions.getAsBytes());
        return buffer.array();
    }
}
