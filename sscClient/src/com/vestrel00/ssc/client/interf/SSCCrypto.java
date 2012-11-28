package com.vestrel00.ssc.client.interf;

/**
 * Cryptography for the SSC protocol.
 * 
 * @author Estrellado, Vandolf
 * 
 */
public interface SSCCrypto {

	/**
	 * 
	 * @param message
	 *            to encrypt
	 * @return the encrypted message
	 */
	public byte[] encrypt(byte[] message);

	/**
	 * 
	 * @param message
	 *            to decrypt
	 * @param iv
	 *            the initialization vector fed into AES CBC mode
	 * @return the decrypted message
	 */
	public byte[] decrypt(byte[] message, byte[] iv);

	/**
	 * 
	 * @return the stored confirm code.
	 */
	public byte[] getConfirmCode();

}
