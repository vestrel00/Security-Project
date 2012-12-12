package com.vestrel00.ssc.client.interf;

/**
 * Asymmetric cryptography used for pre-chat transactions.
 * 
 * @author Estrellado, Vandolf
 * 
 */
public interface SSCCryptoPublic {

	/**
	 * 
	 * @param message
	 *            to encrypt
	 * 
	 * @return the encrypted message
	 */
	public byte[] encrypt(byte[] message);

	/**
	 * 
	 * @param message
	 *            to decrypt
	 * 
	 * @return the decrypted message
	 */
	public byte[] decrypt(byte[] message);

}
