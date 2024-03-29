package com.vestrel00.ssc.client.shared;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.vestrel00.ssc.client.interf.SSCCryptoPublic;

public class SSCCryptoRSA implements SSCCryptoPublic {

	private PrivateKey privKey;
	private PublicKey pubKey;
	private Cipher cipher;

	public SSCCryptoRSA(PrivateKey privKey, PublicKey pubKey) {
		this.privKey = privKey;
		this.pubKey = pubKey;
	}

	@Override
	public byte[] encrypt(byte[] message) {
		try {
			cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE, pubKey);
			return cipher.doFinal(message);
		} catch (IllegalBlockSizeException | BadPaddingException
				| NoSuchAlgorithmException | NoSuchPaddingException
				| InvalidKeyException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public byte[] decrypt(byte[] message) {
		try {
			cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.DECRYPT_MODE, privKey);
			return cipher.doFinal(message);
		} catch (IllegalBlockSizeException | BadPaddingException
				| NoSuchAlgorithmException | NoSuchPaddingException
				| InvalidKeyException e) {
			e.printStackTrace();
		}
		return null;
	}

}
