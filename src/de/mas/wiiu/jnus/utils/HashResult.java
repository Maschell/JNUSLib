/****************************************************************************
 * Copyright (C) 2016-2018 Maschell
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

import lombok.Data;

@Data
public class HashResult {
    private final byte[] SHA1;
    private final byte[] MD5;
    private final byte[] CRC32;

    @Override
    public String toString() {
        return "HashResult [SHA1=" + Utils.ByteArrayToString(SHA1) + ", MD5=" + Utils.ByteArrayToString(MD5) + ", CRC32=" + Utils.ByteArrayToString(CRC32)
                + "]";
    }
}
