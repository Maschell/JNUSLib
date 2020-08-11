/****************************************************************************
 * Copyright (C) 2016-2019 Maschell
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ****************************************************************************/
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
        ByteBuffer buffer = ByteBuffer.allocate(4).order(bo).putInt(value);
        buffer.position(0);
        buffer.get(result);
        return result;
    }

    public static byte[] getBytesFromShort(short value) {
        byte[] result = new byte[0x02];
        ByteBuffer.allocate(2).putShort(value).get(result);
        return result;
    }

    public static short getByteFromBytes(byte[] input, int offset) {
        ByteBuffer buffer = ByteBuffer.allocate(2).put(Arrays.copyOfRange(input, offset, offset + 1)).order(ByteOrder.BIG_ENDIAN);
        buffer.position(0);
        return (short) ((buffer.getShort() & 0xFF00) >> 8);
    }

}
