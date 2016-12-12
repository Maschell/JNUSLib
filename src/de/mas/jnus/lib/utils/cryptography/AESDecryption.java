package de.mas.jnus.lib.utils.cryptography;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import lombok.Getter;
import lombok.Setter;

public class AESDecryption {	
	private Cipher cipher;
	
	@Getter @Setter private byte[] AESKey;
	@Getter @Setter private byte[] IV;
	
	public AESDecryption(byte[] AESKey, byte[] IV) {	
		try {
		    cipher = Cipher.getInstance("AES/CBC/NoPadding");
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			e.printStackTrace();
		}
		setAESKey(AESKey);
		setIV(IV);
		init();	
	}
    
    protected void init() {
        init(getAESKey(),getIV());
    }
	
    protected void init(byte[] decryptedKey,byte[] iv){
		SecretKeySpec secretKeySpec = new SecretKeySpec(decryptedKey, "AES");
		try {
		    cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(iv));
		} catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
			e.printStackTrace();
			System.exit(2);
		}
	}
	
    public byte[] decrypt(byte[] input){
		try {
			return cipher.doFinal(input);			
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
			System.exit(2);
		}
		return input;	
	}
	
    public byte[] decrypt(byte[] input,int len){
		return decrypt(input,0,len);
	}
	
    public byte[] decrypt(byte[] input,int offset,int len){
		try {			
			return cipher.doFinal(input, offset, len);			
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
			System.exit(2);
		}
		return input;	
	}
}
