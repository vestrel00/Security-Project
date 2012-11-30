package com.vestrel00.ssc.server.interf;

/**
 * Symmetric key Cryptography for the SSC protocol.
 * 
 * @author Estrellado, Vandolf
 * 
 */
public interface SSCCryptoPrivate {

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

	/**
	 * 
	 * @return the initialization vector saved from the last most call to
	 *         encrypt.
	 */
	public byte[] getIv();

}
