package de.mas.wiiu.jnus.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public final class ByteUtils {

    private ByteUtils() {
        // Utility Class
    }

    public static int getIntFromBytes(byte[] input, int offset) {
        return getIntFromBytes(input, offset, ByteOrder.BIG_ENDIAN);
    }

    public static int getIntFromBytes(byte[] input, int offset, ByteOrder bo) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(bo);
        Arrays.copyOfRange(input, offset, offset + 4);
        buffer.put(Arrays.copyOfRange(input, offset, offset + 4));

        return buffer.getInt(0);
    }

    public static long getUnsingedIntFromBytes(byte[] input, int offset) {
        return getUnsingedIntFromBytes(input, offset, ByteOrder.BIG_ENDIAN);
    }

    public static long getUnsingedIntFromBytes(byte[] input, int offset, ByteOrder bo) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.order(bo);
        if (bo.equals(ByteOrder.BIG_ENDIAN)) {
            buffer.position(4);
        } else {
            buffer.position(0);
        }
        buffer.put(Arrays.copyOfRange(input, offset, offset + 4));

        return buffer.getLong(0);
    }

    public static long getLongFromBytes(byte[] input, int offset) {
        return getLongFromBytes(input, offset, ByteOrder.BIG_ENDIAN);
    }

    public static long getLongFromBytes(byte[] input, int offset, ByteOrder bo) {
        return ByteBuffer.wrap(Arrays.copyOfRange(input, offset, offset + 8)).order(bo).getLong(0);
    }

    public static short getShortFromBytes(byte[] input, int offset) {
        return getShortFromBytes(input, offset, ByteOrder.BIG_ENDIAN);
    }

    public static short getShortFromBytes(byte[] input, int offset, ByteOrder bo) {
        return ByteBuffer.wrap(Arrays.copyOfRange(input, offset, offset + 2)).order(bo).getShort();
    }

    public static byte[] getBytesFromLong(long value) {
        return getBytesFromLong(value, ByteOrder.BIG_ENDIAN);
    }

    public static byte[] getBytesFromLong(long value, ByteOrder bo) {
        byte[] result = new byte[0x08];
        ByteBuffer.allocate(8).order(bo).putLong(value).get(result);
        return result;
    }

    public static byte[] getBytesFromInt(int value) {
        return getBytesFromInt(value, ByteOrder.BIG_ENDIAN);
    }

    public static byte[] getBytesFromInt(int value, ByteOrder bo) {
        byte[] result = new byte[0x04];
        ByteBuffer.allocate(4).order(bo).putInt(value).get(result);
        return result;
    }

    public static byte[] getBytesFromShort(short value) {
        byte[] result = new byte[0x02];
        ByteBuffer.allocate(2).putShort(value).get(result);
        return result;
    }

}
