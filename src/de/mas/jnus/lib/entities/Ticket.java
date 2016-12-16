package de.mas.jnus.lib.entities;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Arrays;

import de.mas.jnus.lib.Settings;
import de.mas.jnus.lib.utils.Utils;
import de.mas.jnus.lib.utils.cryptography.AESDecryption;
import lombok.Getter;
import lombok.extern.java.Log;

@Log
public final class Ticket {
    @Getter private final byte[] encryptedKey;
    @Getter private final byte[] decryptedKey;
    
    @Getter private final byte[] IV;

    private Ticket(byte[] encryptedKey, byte[] decryptedKey,byte[] IV) {
        super();
        this.encryptedKey = encryptedKey;
        this.decryptedKey = decryptedKey;
        this.IV = IV;
    }
    
    public static Ticket parseTicket(File ticket) throws IOException {
        if(ticket == null || !ticket.exists()){
            log.warning("Ticket input file null or doesn't exist.");
            return null;
        }
        return parseTicket(Files.readAllBytes(ticket.toPath()));
    }
    
    public static Ticket parseTicket(byte[] ticket) throws IOException {
        if(ticket == null){
            return null;
        }
       
        ByteBuffer buffer = ByteBuffer.allocate(ticket.length);
        buffer.put(ticket);
        
        //read key
        byte[] encryptedKey = new byte[0x10];
        buffer.position(0x1BF);
        buffer.get(encryptedKey,0x00,0x10);
        
        //read titleID
        buffer.position(0x1DC);
        long titleID = buffer.getLong();
        
        Ticket result = createTicket(encryptedKey,titleID);
        
        return result;
    }
    
    public static Ticket createTicket(byte[] encryptedKey, long titleID) {
        byte[] IV = ByteBuffer.allocate(0x10).putLong(titleID).array();
        byte[] decryptedKey = calculateDecryptedKey(encryptedKey,IV);
        
        return new Ticket(encryptedKey,decryptedKey,IV);
    }

    private static byte[] calculateDecryptedKey(byte[] encryptedKey, byte[] IV) {        
        AESDecryption decryption = new AESDecryption(Settings.commonKey, IV){};
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
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Ticket other = (Ticket) obj;
        return Arrays.equals(encryptedKey, other.encryptedKey);
    }

    @Override
    public String toString() {
        return "Ticket [encryptedKey=" + Utils.ByteArrayToString(encryptedKey) + ", decryptedKey="
                + Utils.ByteArrayToString(decryptedKey) + "]";
    }
}
