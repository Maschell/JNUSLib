package de.mas.jnus.lib.entities.content;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.mas.jnus.lib.Settings;
import de.mas.jnus.lib.entities.fst.FSTEntry;
import de.mas.jnus.lib.utils.Utils;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a Content
 * @author Maschell
 *
 */
public class Content{
	public static final short CONTENT_HASHED = 0x0002; 
	public static final short CONTENT_ENCRYPTED = 0x0001; 
	
	@Getter @Setter private int ID = 0x00;
	@Getter @Setter private short index = 0x00;		
	@Getter @Setter private short type = 0x0000;
		
	@Getter @Setter private long encryptedFileSize = 0;
	@Getter @Setter private byte[] SHA2Hash = new byte[0x14];

    @Getter private List<FSTEntry> entries = new ArrayList<>();
        
    @Getter @Setter private ContentFSTInfo contentFSTInfo = null;

    /**
     * Creates a new Content object given be the raw byte data
     * @param input 0x30 byte of data from the TMD (starting at 0xB04)
     * @return content object
     */
    public static Content parseContent(byte[] input) {
        if(input == null || input.length != 0x30){
            System.out.println("Error: invalid Content byte[] input");
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate(input.length);
        buffer.put(input);
        buffer.position(0);
        int ID = buffer.getInt(0x00);
        short index = buffer.getShort(0x04);
        short type = buffer.getShort(0x06);
        long encryptedFileSize =  buffer.getLong(0x08);
        buffer.position(0x10);
        byte[] hash =  new byte[0x14];
        buffer.get(hash, 0x00, 0x14);
        return new Content(ID, index,type,encryptedFileSize, hash);  
    }
    
    public Content(int ID, short index, short type, long encryptedFileSize, byte[] hash) {
        setID(ID);
        setIndex(index);
        setType(type);
        setEncryptedFileSize(encryptedFileSize);
        setSHA2Hash(hash);
    }   
    
    /**
     * Returns if the content is hashed
     * @return true if hashed
     */
    public boolean isHashed() {        
        return (type & CONTENT_HASHED) == CONTENT_HASHED;
    }
    /**
     * Returns if the content is encrypted
     * @return true if encrypted
     */
    public boolean isEncrypted() {        
        return (type & CONTENT_ENCRYPTED) == CONTENT_ENCRYPTED;
    }
    
    /**
     * Return the filename of the encrypted content. 
     * It's the ID as hex with an extension 
     * For example: 00000000.app
     * @return filename of the encrypted content
     */
    public String getFilename(){
        return String.format("%08X%s", getID(),Settings.ENCRYPTED_CONTENT_EXTENTION);
    }

    /**
     * Adds a content to the internal entry list.
     * @param entry that will be added to the content list
     */
    public void addEntry(FSTEntry entry) {
        getEntries().add(entry);
    }
    
    /**
     * Returns the size of the decrypted content.
     * @return size of the decrypted content
     */
    public long getDecryptedFileSize() {
        if(isHashed()){
            return getEncryptedFileSize()/0x10000*0xFC00;
        }else{
            return getEncryptedFileSize();
        }
    }
  
    /**
     * Return the filename of the decrypted content. 
     * It's the ID as hex with an extension 
     * For example: 00000000.dec
     * @return filename of the decrypted content
     */
    public String getFilenameDecrypted() {
        return String.format("%08X%s", getID(),Settings.DECRYPTED_CONTENT_EXTENTION);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ID;
        result = prime * result + Arrays.hashCode(SHA2Hash);
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
        Content other = (Content) obj;
        if (ID != other.ID)
            return false;
        if (!Arrays.equals(SHA2Hash, other.SHA2Hash))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Content [ID=" + Integer.toHexString(ID) + ", index=" + Integer.toHexString(index) + ", type=" + String.format("%04X", type) + ", encryptedFileSize=" + encryptedFileSize + ", SHA2Hash=" + Utils.ByteArrayToString(SHA2Hash) + "]";
    }
}