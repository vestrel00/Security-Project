package com.vestrel00.ssc.server.shared;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.vestrel00.ssc.server.interf.SSCCrypto;

/**
 * Simple asymmetric crypto using AES in CBC mode
 * 
 * @author Estrellado, Vandolf
 * 
 */
public class SSCCryptoAES implements SSCCrypto {

	private Cipher cipher;
	private SecretKeySpec spec;
	private byte[] key, confirmCode;

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
	public SSCCryptoAES(byte[] key, byte[] confirmCode)
			throws IllegalArgumentException {
		if (key.length != 16)
			throw new IllegalArgumentException("length of key must = 16");
		this.key = key;
		this.confirmCode = confirmCode;
	}

	@Override
	public byte[] decrypt(byte[] message, byte[] iv) {
		try {
			cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			spec = new SecretKeySpec(key, "AES");
			cipher.init(Cipher.DECRYPT_MODE, spec, new IvParameterSpec(iv));
			return cipher.doFinal(message);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException
				| InvalidKeyException | IllegalBlockSizeException
				| BadPaddingException | InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public byte[] getConfirmCode() {
		return confirmCode;
	}

}
