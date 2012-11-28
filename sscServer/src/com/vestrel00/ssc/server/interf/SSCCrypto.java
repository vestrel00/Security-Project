package com.vestrel00.ssc.server.interf;

/**
 * Cryptography for the SSC protocol.
 * Encryption is unused for the server.
 * 
 * @author Estrellado, Vandolf
 * 
 */
public interface SSCCrypto {

	/**
	 * 
	 * @param message
	 *            to decrypt
	 * @param iv
	 *            the initialization vector for AES CBC mode
	 * @return the decrypted message
	 */
	public byte[] decrypt(byte[] message, byte[] iv);

	/**
	 * 
	 * @return the stored confirm code.
	 */
	public byte[] getConfirmCode();

}
