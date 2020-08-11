package de.mas.wiiu.jnus.implementations.wud;

import java.io.IOException;
import java.util.Optional;

import de.mas.wiiu.jnus.implementations.wud.header.WiiUDiscHeader;
import de.mas.wiiu.jnus.implementations.wud.reader.WUDDiscReader;
import lombok.Data;

@Data
public class WiiUDisc {
    public final static long DISC_SIZE = 25025314816L;
    private WiiUDiscHeader header = new WiiUDiscHeader();
    private Optional<WUDDiscReader> reader;
    private Optional<byte[]> discKey;

    public static WiiUDisc parseData(WUDDiscReader reader, Optional<byte[]> discKey) throws IOException {
        WiiUDisc disc = new WiiUDisc();
        disc.setReader(Optional.of(reader));
        disc.setHeader(WiiUDiscHeader.parseData(reader, discKey));
        disc.setDiscKey(discKey);
        return disc;
    }

    public byte[] getAsBytes() throws IOException {
        return header.getAsBytes();
    }
}
