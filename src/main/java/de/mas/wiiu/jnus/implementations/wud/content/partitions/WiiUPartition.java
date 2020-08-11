package de.mas.wiiu.jnus.implementations.wud.content.partitions;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import de.mas.wiiu.jnus.implementations.wud.content.partitions.volumes.VolumeHeader;
import de.mas.wiiu.jnus.implementations.wud.reader.WUDDiscReader;
import de.mas.wiiu.jnus.utils.ByteUtils;
import de.mas.wiiu.jnus.utils.blocksize.AddressInDiscBlocks;
import de.mas.wiiu.jnus.utils.blocksize.DiscBlockSize;
import lombok.Data;
import lombok.val;

@Data
public class WiiUPartition {
    public static final int LENGTH = 128;
    private final Map<AddressInDiscBlocks, VolumeHeader> volumes = new HashMap<>();
    private String volumeID;
    private short fileSystemDescriptor;

    public static WiiUPartition parseData(WUDDiscReader reader, Optional<byte[]> discKey, long offset, DiscBlockSize blockSize) throws IOException {
        byte[] rawBytes = new byte[0];
        if (!discKey.isPresent()) {
            rawBytes = Arrays.copyOfRange(reader.readEncryptedToByteArray(offset, 0, LENGTH), 0, LENGTH);
        } else {
            // Hacky solution for not knowing the correct IV.
            rawBytes = Arrays.copyOfRange(reader.readDecryptedToByteArray(offset - 0x10, 0, LENGTH, discKey.get(), null, true), 0x10, LENGTH + 0x10);
        }

        if (rawBytes.length != LENGTH) {
            throw new IOException("Failed to read WiiUPartition");
        }

        ByteBuffer buffer = ByteBuffer.wrap(rawBytes);

        byte[] strRaw = Arrays.copyOfRange(rawBytes, 0, 31);
        for (int i = 0; i < strRaw.length; i++) {
            if (strRaw[i] == '\0') {
                strRaw = Arrays.copyOf(strRaw, i);
                break;
            }
        }

        String volumeID = new String(strRaw, Charset.forName("ISO-8859-1"));
        buffer.position(31);
        int num = (int) buffer.get();

        WiiUPartition partition = new WiiUPartition();
        for (int i = 0; i < num; i++) {
            AddressInDiscBlocks discLbaAddress = new AddressInDiscBlocks(blockSize,
                    ByteUtils.getUnsingedIntFromBytes(rawBytes, 32 + (int) (i * 4)) & 0xFFFFFFFF);
            VolumeHeader vh = VolumeHeader.parseData(reader, discLbaAddress.getAddressInBytes());
            partition.volumes.put(discLbaAddress, vh);
        }
        buffer.position(64);
        short fileSystemDescriptor = buffer.getShort();

        partition.setVolumeID(volumeID);
        partition.setFileSystemDescriptor(fileSystemDescriptor);

        return partition;
    }

    public long getSectionOffsetOnDefaultPartition() throws IOException {
        if (getVolumes().size() != 1) {
            throw new IOException("We have more or less than 1 volume header.");
        }
        return getVolumes().keySet().iterator().next().getAddressInBytes();
    }

    public byte[] getAsBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(LENGTH);
        buffer.put(volumeID.getBytes());
        buffer.position(31);
        buffer.put((byte) volumes.size());
        for (val address : volumes.entrySet()) {
            buffer.putInt((int) address.getKey().getValue());
        }
        buffer.position(64);
        buffer.putShort(fileSystemDescriptor);
        return buffer.array();
    }

}
