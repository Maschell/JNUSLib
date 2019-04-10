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
package de.mas.wiiu.jnus.entities;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Arrays;

import de.mas.wiiu.jnus.utils.Utils;
import de.mas.wiiu.jnus.utils.cryptography.AESDecryption;
import lombok.Getter;
import lombok.extern.java.Log;

@Log
public final class Ticket {
    private static final int POSITION_KEY = 0x1BF;
    private static final int POSITION_TITLEID = 0x1DC;

    @Getter private final byte[] encryptedKey;
    @Getter private final byte[] decryptedKey;

    @Getter private final byte[] IV;

    private Ticket(byte[] encryptedKey, byte[] decryptedKey, byte[] IV) {
        this.encryptedKey = encryptedKey;
        this.decryptedKey = decryptedKey;
        this.IV = IV;
    }

    public static Ticket parseTicket(File ticket, byte[] commonKey) throws IOException {
        if (ticket == null || !ticket.exists()) {
            log.warning("Ticket input file null or doesn't exist.");
            throw new IOException("Ticket input file null or doesn't exist.");
        }
        return parseTicket(Files.readAllBytes(ticket.toPath()), commonKey);
    }

    public static Ticket parseTicket(byte[] ticket, byte[] commonKey) throws IOException {
        if (ticket == null) {
            throw new IOException("Ticket input file null or doesn't exist.");
        }

        ByteBuffer buffer = ByteBuffer.allocate(ticket.length);
        buffer.put(ticket);

        // read key
        byte[] encryptedKey = new byte[0x10];
        buffer.position(POSITION_KEY);
        buffer.get(encryptedKey, 0x00, 0x10);

        // read titleID
        buffer.position(POSITION_TITLEID);
        long titleID = buffer.getLong();

        Ticket result = createTicket(encryptedKey, titleID, commonKey);

        return result;
    }

    public static Ticket createTicket(byte[] encryptedKey, long titleID, byte[] commonKey) {
        byte[] IV = ByteBuffer.allocate(0x10).putLong(titleID).array();
        byte[] decryptedKey = calculateDecryptedKey(encryptedKey, IV, commonKey);

        return new Ticket(encryptedKey, decryptedKey, IV);
    }

    private static byte[] calculateDecryptedKey(byte[] encryptedKey, byte[] IV, byte[] commonKey) {
        AESDecryption decryption = new AESDecryption(commonKey, IV);
        return decryption.decrypt(encryptedKey);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(encryptedKey);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Ticket other = (Ticket) obj;
        return Arrays.equals(encryptedKey, other.encryptedKey);
    }

    @Override
    public String toString() {
        return "Ticket [encryptedKey=" + Utils.ByteArrayToString(encryptedKey) + ", decryptedKey=" + Utils.ByteArrayToString(decryptedKey) + "]";
    }

}
