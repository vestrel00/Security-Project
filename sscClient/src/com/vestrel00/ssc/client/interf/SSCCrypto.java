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
	 * @return the decrypted message
	 */
	public byte[] decrypt(byte[] message);

}