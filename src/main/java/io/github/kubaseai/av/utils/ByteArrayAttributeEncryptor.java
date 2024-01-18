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
public class ByteArrayAttributeEncryptor implements AttributeConverter<byte[], byte[]> {

    private static final String AES = "AES";
    private final Key key;
    private final boolean enabled;
    private ThreadLocal<Cipher> encryptorRef = new ThreadLocal<>();
    private ThreadLocal<Cipher> decryptorRef = new ThreadLocal<>();

    public ByteArrayAttributeEncryptor(MainConfiguration configuration) {
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
    
    private byte[] nonNull(byte[] arr) {
		return arr==null ? new byte[0] : arr;
	}
    
    public byte[] convertToDatabaseColumn(byte[] attribute) {
    	if (!enabled) {
    		return attribute;
    	}
        try {
        	attribute = nonNull(attribute);
            return Base64.getEncoder().encode(cipher(true).doFinal(attribute));
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }    

	public byte[] convertToEntityAttribute(byte[] dbData) {
		if (!enabled) {
    		return dbData;
    	}
        try {
            byte[] arr = cipher(false).doFinal(Base64.getDecoder().decode(dbData));
            return arr!=null && arr.length == 0 ? null: arr;
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }
}
