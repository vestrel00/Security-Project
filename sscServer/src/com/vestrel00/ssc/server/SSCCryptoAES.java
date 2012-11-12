package com.vestrel00.ssc.server;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import com.vestrel00.ssc.server.interf.SSCCrypto;

/**
 * Simple asymmetric crypto using AES.
 * 
 * @author Estrellado, Vandolf
 * 
 */
public class SSCCryptoAES implements SSCCrypto {

	private Cipher cipher;
	private SecretKeySpec spec;
	private byte[] key;

	/**
	 * Create the crypto instance.
	 * 
	 * @param key
	 *            should be given by server to client and stored in a database
	 *            in encrypted form with salt. The key is a byte array of length
	 *            16.
	 * @throws InvalidKeyLengthException
	 */
	public SSCCryptoAES(byte[] key) throws IllegalArgumentException {
		if (key.length != 16)
			throw new IllegalArgumentException("length of key must = 16");
		this.key = key;
	}

	public byte[] encrypt(byte[] message) {
		try {
			perform(Cipher.ENCRYPT_MODE);
			return cipher.doFinal(message);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException
				| InvalidKeyException | IllegalBlockSizeException
				| BadPaddingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public byte[] decrypt(byte[] message) {
		try {
			perform(Cipher.DECRYPT_MODE);
			return cipher.doFinal(message);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException
				| InvalidKeyException | IllegalBlockSizeException
				| BadPaddingException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void perform(int mode) throws NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidKeyException {
		cipher = Cipher.getInstance("AES");
		spec = new SecretKeySpec(key, "AES");
		cipher.init(mode, spec);
	}

}