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
import lombok.Setter;
import lombok.extern.java.Log;

@Log
public class Ticket {
    @Getter @Setter private byte[] encryptedKey = new byte[0x10];
    @Getter @Setter private byte[] decryptedKey = new byte[0x10];
    
    @Getter @Setter private byte[] IV = new byte[0x10];
    
    @Getter @Setter private byte[] cert0 = new byte[0x300];
    @Getter @Setter private byte[] cert1 = new byte[0x400];

    @Getter @Setter private byte[] rawTicket = new byte[0];
    
    private Ticket(){
        
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
        result.setRawTicket(Arrays.copyOf(ticket, ticket.length));
        
        //read certs.
        byte[] cert0 = new byte[0x300];
        byte[] cert1 = new byte[0x400];
        
        if(ticket.length >= 0x650){
            buffer.position(0x350);
            buffer.get(cert0,0x00,0x300);
        }
        if(ticket.length >= 0xA50){
            buffer.position(0x650);
            buffer.get(cert1,0x00,0x400);
        }
        
        result.setCert0(cert0);
        result.setCert1(cert1);
        
        return result;
    }
    
    public static Ticket createTicket(byte[] encryptedKey, long titleID) {
        Ticket result = new Ticket();
        result.encryptedKey = encryptedKey;
        
        byte[] IV = ByteBuffer.allocate(0x10).putLong(titleID).array();
        result.decryptedKey = calculateDecryptedKey(result.encryptedKey,IV);
        result.setIV(IV);
        
        return result;
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
        if (!Arrays.equals(encryptedKey, other.encryptedKey))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Ticket [encryptedKey=" + Utils.ByteArrayToString(encryptedKey) + ", decryptedKey="
                + Utils.ByteArrayToString(decryptedKey) + "]";
    }
}
