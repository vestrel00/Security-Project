package com.vestrel00.ssc.client.protocols;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.vestrel00.ssc.client.SSCClient;
import com.vestrel00.ssc.client.interf.SSCCrypto;
import com.vestrel00.ssc.client.interf.SSCProtocol;
import com.vestrel00.ssc.client.shared.SSCCryptoAES;
import com.vestrel00.ssc.client.shared.SSCStreamManager;

/**
 * This is the protocol that the client program runs.
 * 
 * @author Estrellado, Vandolf
 * 
 */
public class SSCClientProtocol implements SSCProtocol {

	private SSCClient client;
	private SSCCrypto crypt;
	private boolean isWorking;

	public SSCClientProtocol(SSCClient client) {
		this.client = client;
	}

	/**
	 * Initialize the protocol including the crypto.
	 */
	public SSCClientProtocol(SSCClient client, String secretKey,
			String keyCodeOK) {
		this(client);
		initCrypto(secretKey, keyCodeOK);
	}

	@Override
	public void initCrypto(String secretKey, String keyCodeOK) {
		try {
			crypt = new SSCCryptoAES(secretKey.getBytes(), keyCodeOK);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean work() {
		if (isWorking) {
			performMagic();
			return true;
		}
		return false;
	}

	/**
	 * Initialize the protocol and starts listening for SERVER input. Listens
	 * for E(m) and H(m) which the server forwards from user to user. If
	 * H(D(E(m)) == H(m) store the message in the buffer and print out to user.
	 * Otherwise, it has been tampered - ignore it.
	 */
	@Override
	public void performMagic() {
		// Wait for E(m)
		byte[] em;
		try {
			em = SSCStreamManager.readBytes(client.getServerInputStream());
			// tell server that it has been received
			SSCStreamManager.sendBytes(client.getOutputStream(),
					crypt.encrypt(SSCCryptoAES.OK.getBytes()));
			// Wait for H(m)
			byte[] hm = SSCStreamManager.readBytes(client
					.getServerInputStream());
			// m
			byte[] m = crypt.decrypt(em);
			// H(E(m))
			byte[] hem = MessageDigest.getInstance("SHA-1").digest(m);
			// authenticate
			for (int index = 0; index < hm.length; index++) {
				if (hm[index] != hem[index]) // E(m) was tampered with
					continue;
			}
			// Everything checked out
			client.getBuffer().add(new String(m));
		} catch (IOException | NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stopWorking() {
		isWorking = false;
	}
	
	@Override
	public SSCCrypto getCrypto() {
		return crypt;
	}

}
