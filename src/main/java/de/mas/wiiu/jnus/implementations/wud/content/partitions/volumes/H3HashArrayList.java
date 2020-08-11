package de.mas.wiiu.jnus.implementations.wud.content.partitions.volumes;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

import de.mas.wiiu.jnus.implementations.wud.reader.WUDDiscReader;
import de.mas.wiiu.jnus.utils.ByteUtils;

public class H3HashArrayList extends LinkedList<H3HashArray> {
    private static final long serialVersionUID = -7607430979726338763L;

    public static H3HashArrayList parseData(WUDDiscReader reader, long offset, long numberOfH3HashArray, long h3HashArrayListSize) throws IOException {
        byte[] h3Data = reader.readEncryptedToByteArray(offset, 0, h3HashArrayListSize);

        return parseData(h3Data, numberOfH3HashArray, h3HashArrayListSize);
    }

    public static H3HashArrayList parseData(byte[] h3Data, long numberOfH3HashArray, long h3HashArrayListSize) throws IOException {
        H3HashArrayList arrayList = new H3HashArrayList();
        for (int i = 1; i < numberOfH3HashArray; i++) {
            long curOffset = ByteUtils.getUnsingedIntFromBytes(Arrays.copyOfRange(h3Data, i * 4, (i + 1) * 4), 0) & 0xFFFFFFFF;
            long curEnd = h3HashArrayListSize;
            if (i < numberOfH3HashArray - 1) {
                // If it's not the last element, the end of our .h3 is the start of the next .h3
                curEnd = ByteUtils.getUnsingedIntFromBytes(Arrays.copyOfRange(h3Data, (i + 1) * 4, (i + 2) * 4), 0) & 0xFFFFFFFF;
            }
            arrayList.add(new H3HashArray(Arrays.copyOfRange(h3Data, (int) curOffset, (int) curEnd)));
        }

        return arrayList;
    }
}
