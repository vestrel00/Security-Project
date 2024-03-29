package com.vestrel00.ssc.client.protocols;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.vestrel00.ssc.client.SSCClient;
import com.vestrel00.ssc.client.interf.SSCCryptoPrivate;
import com.vestrel00.ssc.client.interf.SSCProtocol;
import com.vestrel00.ssc.client.shared.SSCStreamManager;

/**
 * This is the protocol that the client program runs dedicated to receiving
 * incoming messages from the service.
 * 
 * @author Estrellado, Vandolf
 * 
 */
public class SSCClientMessageReceiver implements SSCProtocol {

	private SSCClient client;
	private SSCCryptoPrivate crypt;

	public SSCClientMessageReceiver(SSCClient client, SSCCryptoPrivate crypt) {
		this.client = client;
		this.crypt = crypt;
	}

	@Override
	public boolean work() {
		if (client.isRunning()) {
			performMagic();
			return true;
		}
		return false;
	}

	/**
	 * MessageListener: Listens for E(m) and H(m) which the server forwards from
	 * user to user. If H(D(E(m)) == H(m) store the message in the buffer and
	 * print out to user. Otherwise, it has been tampered - ignore it.
	 */
	@Override
	public void performMagic() {
		try {
			// System.out.println(client.getUserName()
			// + "Receiver: waiting for E(m)");
			// Wait for E(m)
			byte[] em = SSCStreamManager.readBytes(client
					.getServerInputStream());
			// System.out.println(client.getUserName()
			// + "Receiver: sending confirmCode");
			// send confirmCode
			SSCStreamManager.sendBytes(client.getOutputStream(),
					crypt.getConfirmCode());
			// System.out.println(client.getUserName()
			// + "Receiver: waiting for IV");
			// Wait for IV
			byte[] iv = SSCStreamManager.readBytes(client
					.getServerInputStream());
			// System.out.println(client.getUserName()
			// + "Receiver: sending confirmCode");
			// send confirmCode
			SSCStreamManager.sendBytes(client.getOutputStream(),
					crypt.getConfirmCode());
			// System.out.println(client.getUserName()
			// + "Receiver: waiting for H(E(m))");
			// Wait for H(E(m))
			byte[] hem = SSCStreamManager.readBytes(client
					.getServerInputStream());
			// m
			byte[] m = crypt.decrypt(em, iv);
			// H(E(m))
			byte[] hm = MessageDigest.getInstance("SHA-1").digest(em);
			
			if (hem.length != hm.length)
				return;

			boolean confirmed = true;
			for (int index = 0; index < hm.length; index++) {
				if (hm[index] != hem[index]) { // E(m) or H(m) was tampered with
					confirmed = false;
					break;
				}
			}

			if (confirmed) {
				// System.out.println(client.getUserName()
				// + "Receiver: everything checked out");
				// Everything checked out
				String mStr = new String(m);
				client.getBuffer().add(mStr);
				System.out.println(mStr);
			} else {
				System.out.println("Warning! Connection may be compromised.");
			}
		} catch (IOException | NoSuchAlgorithmException e) {
			try {
				client.finish();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

}
