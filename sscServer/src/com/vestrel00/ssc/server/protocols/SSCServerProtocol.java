package com.vestrel00.ssc.server.protocols;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.vestrel00.ssc.server.shared.SSCCryptoAES;
import com.vestrel00.ssc.server.shared.SSCStreamManager;
import com.vestrel00.ssc.server.interf.SSCCrypto;
import com.vestrel00.ssc.server.interf.SSCProtocol;
import com.vestrel00.ssc.server.interf.SSCServerService;

/**
 * This is the protocol that the server runs for each client.
 * 
 * @author Estrellado, Vandolf
 * 
 */
public class SSCServerProtocol implements SSCProtocol {

	private SSCServerService service;
	private SSCCrypto crypt;
	private boolean isWorking;

	/**
	 * Initialize the protocol as well as the crypto.
	 * 
	 * @param service
	 *            the service that launched this protocol.
	 * @param serverBufferId
	 * @param secretKey
	 *            the key that the destination client and the client of this
	 *            service agreed upon.
	 * @param confirmCode
	 *            the keyCode agreed upon by the destination client and this
	 *            service's client.
	 * 
	 * @param service
	 *            the service that launched this protocol.
	 */
	public SSCServerProtocol(SSCServerService service, byte[] secretKey,
			byte[] confirmCode) {
		this.service = service;
		isWorking = true;
		crypt = new SSCCryptoAES(secretKey, confirmCode);
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
	 * Wait for E(m) and H(m) from the client. Authenticate and if everything
	 * checks out, store m in the client's buffer and forward E(m) and H(m) to
	 * the service which handles the destination client.
	 */
	@Override
	public void performMagic() {
		try {
			// Wait for E(m)
			byte[] em = SSCStreamManager.readBytes(service.getInputStream());
			// tell client that it has been received
			SSCStreamManager.sendBytes(service.getOutputStream(),
					crypt.encrypt(crypt.getConfirmCode()));
			// Wait for H(m)
			byte[] hm = SSCStreamManager.readBytes(service.getInputStream());
			// m
			byte[] m = crypt.decrypt(em);
			// H(E(m))
			byte[] hem = MessageDigest.getInstance("SHA-1").digest(m);
			// authenticate
			if (hem.length != hm.length)
				return;
			for (int index = 0; index < hm.length; index++) {
				if (hm[index] != hem[index]) // E(m) was tampered with
					return;
			}
			// Everything checked out
			service.getClientBuffer().addMessage(m);
			service.forwardMessageToService(service.getOtherClientService(),
					hem, hm);
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
