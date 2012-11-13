package com.vestrel00.ssc.client.shared;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import com.vestrel00.ssc.client.interf.SSCCrypto;

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
	private String confirmCode;

	/**
	 * Create the crypto instance. All parameters must be the same as the
	 * server's crypto parameters.
	 * 
	 * @param key
	 *            should be given by server to client and stored in a database
	 *            in encrypted form with salt. The key is a byte array of length
	 *            16.
	 * @param confirmCode
	 *            an instance of the confirmCode
	 * 
	 * @throws InvalidKeyLengthException
	 */
	public SSCCryptoAES(byte[] key, String confirmCode)
			throws IllegalArgumentException {
		if (key.length != 16)
			throw new IllegalArgumentException("length of key must = 16");
		this.key = key;
		this.confirmCode = confirmCode;
	}

	@Override
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

	@Override
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

	@Override
	public String getConfirmCode() {
		return confirmCode;
	}

}
