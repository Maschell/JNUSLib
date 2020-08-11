package de.mas.wiiu.jnus.implementations.wud.content.partitions.volumes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Callable;

import de.mas.wiiu.jnus.implementations.wud.reader.WUDDiscReader;
import de.mas.wiiu.jnus.utils.ByteUtils;
import de.mas.wiiu.jnus.utils.blocksize.AddressInVolumeBlocks;
import de.mas.wiiu.jnus.utils.blocksize.SizeInVolumeBlocks;
import de.mas.wiiu.jnus.utils.blocksize.VolumeBlockSize;
import lombok.Data;

@Data
public class VolumeHeader {
    public static final int MAGIC = 0xCC93A4F5;

    private VolumeBlockSize blockSize;
    private SizeInVolumeBlocks volumeSize;

    private long h3HashArrayListSize;
    private long numberOfH3HashArray;

    private long FSTSize;
    private AddressInVolumeBlocks FSTAddress;

    private int FSTHashMode;

    private short encryptType;

    private short majorVersion;

    private short minorVersion;

    private short expiringMajorVersion;

    private H3HashArrayList h3HashArrayList;

    public static VolumeHeader parseData(WUDDiscReader reader, long offset) throws IOException {

        byte[] rawBytes = new byte[0];

        rawBytes = reader.readEncryptedToByteArray(offset, 0, 40);

        long h3HashArrayListSize = ByteUtils.getUnsingedIntFromBytes(rawBytes, 12) & 0xFFFFFFFF;
        long numberOfH3HashArray = ByteUtils.getUnsingedIntFromBytes(rawBytes, 16) & 0xFFFFFFFF;

        return parseData(rawBytes, () -> H3HashArrayList.parseData(reader, offset + 64, numberOfH3HashArray, h3HashArrayListSize));
    }

    public static VolumeHeader parseData(byte[] rawBytes) throws IOException {
        if (rawBytes == null || rawBytes.length < 40) {
            throw new IOException("Failed to read VolumeHeader");
        }

        long h3HashArrayListSize = ByteUtils.getUnsingedIntFromBytes(rawBytes, 12) & 0xFFFFFFFF;
        long numberOfH3HashArray = ByteUtils.getUnsingedIntFromBytes(rawBytes, 16) & 0xFFFFFFFF;

        return parseData(rawBytes,
                () -> H3HashArrayList.parseData(Arrays.copyOfRange(rawBytes, 64, (int) (64 + h3HashArrayListSize)), numberOfH3HashArray, h3HashArrayListSize));
    }

    private static VolumeHeader parseData(byte[] rawBytes, Callable<H3HashArrayList> h3HashSupplier) throws IOException {
        if (rawBytes.length < 40) {
            throw new IOException("Failed to read VolumeHeader");
        }

        VolumeHeader header = new VolumeHeader();

        ByteBuffer buffer = ByteBuffer.wrap(rawBytes);
        byte[] magicCompare = new byte[4];
        buffer.get(magicCompare);
        if (!Arrays.equals(magicCompare, ByteUtils.getBytesFromInt(MAGIC))) {
            throw new IOException("VolumeHeader MAGIC mismatch.");
        }

        header.blockSize = new VolumeBlockSize(ByteUtils.getUnsingedIntFromBytes(rawBytes, 4) & 0xFFFFFFFF);
        header.volumeSize = new SizeInVolumeBlocks(header.blockSize, ByteUtils.getUnsingedIntFromBytes(rawBytes, 8) & 0xFFFFFFFF);
        header.h3HashArrayListSize = ByteUtils.getUnsingedIntFromBytes(rawBytes, 12) & 0xFFFFFFFF;
        header.numberOfH3HashArray = ByteUtils.getUnsingedIntFromBytes(rawBytes, 16) & 0xFFFFFFFF;
        header.FSTSize = ByteUtils.getUnsingedIntFromBytes(rawBytes, 20) & 0xFFFFFFFF;
        header.FSTAddress = new AddressInVolumeBlocks(header.blockSize, ByteUtils.getUnsingedIntFromBytes(rawBytes, 24) & 0xFFFFFFFF);
        header.FSTHashMode = ByteUtils.getByteFromBytes(rawBytes, 36);
        header.encryptType = ByteUtils.getByteFromBytes(rawBytes, 37);
        header.majorVersion = ByteUtils.getByteFromBytes(rawBytes, 38);
        header.minorVersion = ByteUtils.getByteFromBytes(rawBytes, 39);
        header.expiringMajorVersion = ByteUtils.getByteFromBytes(rawBytes, 40);
        try {
            header.h3HashArrayList = h3HashSupplier.call();
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            } else {
                // This should never happen.
                throw new RuntimeException(e);
            }
        }

        return header;
    }

    @Override
    public String toString() {
        return "VolumeHeader [blockSize=" + blockSize + ", volumeSize=" + volumeSize + ", h3HashArrayListSize=" + h3HashArrayListSize + ", numberOfH3HashArray="
                + numberOfH3HashArray + ", FSTSize=" + FSTSize + ", FSTAddress=" + FSTAddress + ", FSTHashMode=" + FSTHashMode + ", encryptType=" + encryptType
                + ", majorVersion=" + majorVersion + ", minorVersion=" + minorVersion + ", expiringMajorVersion=" + expiringMajorVersion + ", byteSource="
                + ", h3HashArrayList=" + h3HashArrayList + "]";
    }

    public byte[] getAsBytes() {
        ByteBuffer buffer = ByteBuffer.allocate((int) blockSize.getBlockSize());
        buffer.put(ByteUtils.getBytesFromInt(MAGIC));
        buffer.putInt((int) blockSize.getBlockSize());
        buffer.putInt((int) volumeSize.getValue());
        buffer.putInt((int) h3HashArrayListSize);
        buffer.putInt((int) numberOfH3HashArray);
        buffer.putInt((int) FSTSize);
        buffer.putInt((int) FSTAddress.getValue());
        buffer.put((byte) FSTHashMode);
        buffer.put((byte) encryptType);
        buffer.put((byte) majorVersion);
        buffer.put((byte) minorVersion);
        buffer.put((byte) expiringMajorVersion);
        buffer.position(64);
        if (h3HashArrayListSize > 0 || numberOfH3HashArray > 0) {
            throw new IllegalArgumentException("Support for packing with h3 hashes is missing.");
        }
        return buffer.array();
    }

}
