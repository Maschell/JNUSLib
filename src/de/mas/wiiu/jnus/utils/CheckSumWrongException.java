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

import lombok.Getter;

@Getter
public class CheckSumWrongException extends Exception {
    /**
     * 
     */
    private static final long serialVersionUID = 5781223264453732269L;
    private final byte[] givenHash;
    private final byte[] expectedHash;

    public CheckSumWrongException(String string, byte[] given, byte[] expected) {
        super(string);
        this.givenHash = given;
        this.expectedHash = expected;

    }
}
