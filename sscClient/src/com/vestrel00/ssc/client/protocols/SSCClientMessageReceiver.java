package com.vestrel00.ssc.client.protocols;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.vestrel00.ssc.client.SSCClient;
import com.vestrel00.ssc.client.interf.SSCCrypto;
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
	private SSCCrypto crypt;

	public SSCClientMessageReceiver(SSCClient client, SSCCrypto crypt) {
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
			//System.out.println(client.getUserName()
				//	+ "Receiver: waiting for E(m)");
			// Wait for E(m)
			byte[] em = SSCStreamManager.readBytes(client
					.getServerInputStream());
			//System.out.println(client.getUserName()
				//	+ "Receiver: sending E(confirmCode)");
			// tell server that it has been received
			SSCStreamManager.sendBytes(client.getOutputStream(),
					crypt.encrypt(crypt.getConfirmCode()));
			//System.out.println(client.getUserName()
				//	+ "Receiver: waiting for H(m)");
			// Wait for H(m)
			byte[] hm = SSCStreamManager.readBytes(client
					.getServerInputStream());
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
			//System.out.println(client.getUserName()
				//	+ "Receiver: everything checked out");
			// Everything checked out
			String mStr = new String(m);
			client.getBuffer().add(mStr);
			System.out.println(mStr);
		} catch (IOException | NoSuchAlgorithmException e) {
			try {
				client.finish();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

}