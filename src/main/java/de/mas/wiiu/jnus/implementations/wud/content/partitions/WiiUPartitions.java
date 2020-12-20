package de.mas.wiiu.jnus.implementations.wud.content.partitions;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import de.mas.wiiu.jnus.entities.FST.FST;
import de.mas.wiiu.jnus.entities.FST.nodeentry.FileEntry;
import de.mas.wiiu.jnus.entities.FST.sectionentry.SectionEntry;
import de.mas.wiiu.jnus.implementations.wud.content.partitions.volumes.VolumeHeader;
import de.mas.wiiu.jnus.implementations.wud.reader.WUDDiscReader;
import de.mas.wiiu.jnus.utils.FSTUtils;
import de.mas.wiiu.jnus.utils.Utils;
import de.mas.wiiu.jnus.utils.blocksize.AddressInDiscBlocks;
import de.mas.wiiu.jnus.utils.blocksize.DiscBlockSize;
import lombok.val;

public class WiiUPartitions extends LinkedList<WiiUPartition> {
    private static final long serialVersionUID = -8822482411628791495L;

    public static final long LENGTH = 30720;

    public static final String WUD_TMD_FILENAME = "title.tmd";
    public static final String WUD_TICKET_FILENAME = "title.tik";
    public static final String WUD_CERT_FILENAME = "title.cert";

    public static WiiUPartitions parseData(WUDDiscReader reader, Optional<byte[]> discKey, long curOffset, long numberOfPartitions, DiscBlockSize blockSize)
            throws IOException {
        WiiUPartitions partitions = new WiiUPartitions();

        List<WiiUPartition> tmp = new LinkedList<>();

        for (int i = 0; i < numberOfPartitions; i++) {
            tmp.add(WiiUPartition.parseData(reader, discKey, curOffset + (i * 128), blockSize));
        }

        Optional<WiiUPartition> siPartitionOpt = tmp.stream().filter(e -> e.getVolumeID().startsWith("SI")).findFirst();

        if (siPartitionOpt.isPresent()) {
            WiiUPartition siPartition = siPartitionOpt.get();
            for (val entry : siPartition.getVolumes().entrySet()) {
                val volumeAddress = entry.getKey();
                val volumeAddressInBytes = volumeAddress.getAddressInBytes();
                val volumeHeader = entry.getValue();
                byte[] fst = null;
                if (!discKey.isPresent()) {
                    fst = reader.readEncryptedToByteArray(volumeAddressInBytes + volumeHeader.getFSTAddress().getAddressInBytes(), 0,
                            volumeHeader.getFSTSize());
                } else {
                    fst = reader.readDecryptedToByteArray(volumeAddressInBytes + volumeHeader.getFSTAddress().getAddressInBytes(), 0, volumeHeader.getFSTSize(),
                            discKey.get(), null, true);
                }

                FST siFST = FST.parseData(fst, 0, volumeHeader.getBlockSize());

                for (val child : siFST.getRootEntry().getDirChildren()) {
                    byte[] rawTIK = getFSTEntryAsByte(child.getFullPath() + '/' + WUD_TICKET_FILENAME, siFST, volumeAddress, volumeHeader, reader, discKey);
                    byte[] rawTMD = getFSTEntryAsByte(child.getFullPath() + '/' + WUD_TMD_FILENAME, siFST, volumeAddress, volumeHeader, reader, discKey);
                    byte[] rawCert = getFSTEntryAsByte(child.getFullPath() + '/' + WUD_CERT_FILENAME, siFST, volumeAddress, volumeHeader, reader, discKey);

                    String partitionName = "GM" + Utils.ByteArrayToString(Arrays.copyOfRange(rawTIK, 0x1DC, 0x1DC + 0x08));
                    WiiUPartition partition = tmp.stream().filter(p -> p.getVolumeID().startsWith(partitionName)).findAny()
                            .orElseThrow(() -> new IOException("Failed to find partition starting with " + partitionName));

                    WiiUGMPartition gmPartition = new WiiUGMPartition(partition, rawTIK, rawTMD, rawCert);
                    partitions.add(gmPartition);
                }
            }

        }
        val nonGMPartitions = tmp.stream().filter(e -> !e.getVolumeID().startsWith("GM")).collect(Collectors.toList());
        for (val partition : nonGMPartitions) {
            if (partition.getVolumes().size() != 1) {
                throw new IOException("We can't handle more or less than one partion address yet.");
            }
            val volumeAddress = partition.getVolumes().keySet().iterator().next();
            VolumeHeader vh = VolumeHeader.parseData(reader, volumeAddress.getAddressInBytes());
            byte[] rawFST = null;
            if (!discKey.isPresent()) {
                rawFST = reader.readEncryptedToByteArray(volumeAddress.getAddressInBytes() + vh.getFSTAddress().getAddressInBytes(), 0, vh.getFSTSize());
            } else {
                rawFST = reader.readDecryptedToByteArray(volumeAddress.getAddressInBytes() + vh.getFSTAddress().getAddressInBytes(), 0, vh.getFSTSize(),
                        discKey.get(), null, true);
            }

            FST fst = FST.parseData(rawFST, 0, vh.getBlockSize());
            partitions.add(new WiiUDataPartition(partition, fst));
        }

        return partitions;
    }

    private static byte[] getFSTEntryAsByte(String filePath, FST fst, AddressInDiscBlocks volumeAddress, VolumeHeader vh, WUDDiscReader discReader,
            Optional<byte[]> discKey) throws IOException {
        FileEntry entry = FSTUtils.getEntryByFullPath(fst.getRootEntry(), filePath).orElseThrow(() -> new FileNotFoundException(filePath + " was not found."));

        SectionEntry info = entry.getSectionEntry();

        long sectionOffsetOnDisc = volumeAddress.getAddressInBytes() + info.getAddress().getAddressInBytes();

        if (!discKey.isPresent()) {
            return discReader.readEncryptedToByteArray(sectionOffsetOnDisc, entry.getOffset(), (int) entry.getSize());
        }

        // Calculating the IV
        ByteBuffer byteBuffer = ByteBuffer.allocate(0x10);
        byteBuffer.position(0x08);
        byte[] IV = byteBuffer.putLong(entry.getOffset() >> 16).array();

        return discReader.readDecryptedToByteArray(sectionOffsetOnDisc, entry.getOffset(), (int) entry.getSize(), discKey.get(), IV, false);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("WiiUPartitions: " + System.lineSeparator());
        for (val p : this) {
            sb.append("[" + p + "]" + System.lineSeparator());
        }
        return sb.toString();
    }

    public byte[] getAsBytes() {
        ByteBuffer buffer = ByteBuffer.allocate((int) LENGTH);
        for (int i = 0; i < size(); i++) {
            buffer.put(get(i).getAsBytes());
        }
        return buffer.array();
    }

}
