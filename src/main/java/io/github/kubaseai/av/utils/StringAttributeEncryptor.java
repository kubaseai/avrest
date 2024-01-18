package io.github.kubaseai.av.utils;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.persistence.AttributeConverter;

import org.springframework.stereotype.Component;

import io.github.kubaseai.av.config.MainConfiguration;

@Component
public class StringAttributeEncryptor implements AttributeConverter<String, String> {

    private static final String AES = "AES";
    private final Key key;
    private final boolean enabled;
    private ThreadLocal<Cipher> encryptorRef = new ThreadLocal<>();
    private ThreadLocal<Cipher> decryptorRef = new ThreadLocal<>();

    public StringAttributeEncryptor(MainConfiguration configuration) throws Exception {
        byte[] passwd = configuration.getDbEncryptionPasswordForBytes();
        key = new SecretKeySpec(passwd, AES);
        enabled = passwd.length > 0;
    }

    private Cipher cipher(boolean encrypt) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
    	Cipher cipher = encrypt ? encryptorRef.get() : decryptorRef.get();
    	if (cipher==null) {
    		cipher = Cipher.getInstance(AES);
    		cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, key);
    		ThreadLocal<Cipher> cipherRef = encrypt ? encryptorRef : decryptorRef;
    		cipherRef.set(cipher);
    	}    	
    	return cipher;
    }
    
    private String nonNull(String s) {
		return s==null ? "" : s;
	}
    
    public String convertToDatabaseColumn(String attribute) {
    	if (!enabled) {
    		return attribute;
    	}
        try {
        	attribute = nonNull(attribute);
            return Base64.getEncoder().encodeToString(cipher(true).doFinal(attribute.getBytes()));
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }   

	public String convertToEntityAttribute(String dbData) {
		if (!enabled) {
    		return dbData;
    	}
        try {
            byte[] b = cipher(false).doFinal(Base64.getDecoder().decode(dbData));
            String s = new String(b);
            return "".equals(s) ? null : s;
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }
}
