package de.mas.wiiu.jnus.implementations.wud.header;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;

import de.mas.wiiu.jnus.implementations.wud.content.WiiUContentsInformation;
import de.mas.wiiu.jnus.implementations.wud.reader.WUDDiscReader;
import lombok.Data;

@Data
public class WiiUDiscHeader {
    private static final long LENGTH = 131072L;
    WiiUManufactorDiscID manufactorDiscID;
    WiiUDiscID discID;
    WiiUContentsInformation contentsInformation;

    public static WiiUDiscHeader parseData(WUDDiscReader reader, Optional<byte[]> discKey) throws IOException {
        WiiUDiscHeader header = new WiiUDiscHeader();
        long curOffset = 0;
        header.setManufactorDiscID(WiiUManufactorDiscID.parseData(reader, 0));
        curOffset += WiiUManufactorDiscID.LENGTH;
        header.setDiscID(WiiUDiscID.parseData(reader, curOffset));
        curOffset += WiiUDiscID.LENGTH;
        header.setContentsInformation(WiiUContentsInformation.parseData(reader, discKey, curOffset));

        curOffset += WiiUContentsInformation.LENGTH;

        if (curOffset != LENGTH) {
            throw new IOException("Length mismatch");
        }

        return header;
    }

    @Override
    public String toString() {
        return "WiiUDiscHeader [manufactorDiscID=" + manufactorDiscID + ", discID=" + discID + ", contentsInformation=" + contentsInformation + "]";
    }

    public byte[] getAsBytes() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate((int) LENGTH);
        buffer.put(manufactorDiscID.getAsBytes());
        buffer.put(discID.getAsBytes());
        buffer.put(contentsInformation.getAsBytes());
        return buffer.array();
    }

}
