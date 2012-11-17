package com.vestrel00.ssc.server.protocols;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import com.vestrel00.ssc.server.shared.SSCStreamManager;
import com.vestrel00.ssc.server.interf.SSCCrypto;
import com.vestrel00.ssc.server.interf.SSCProtocol;
import com.vestrel00.ssc.server.interf.SSCServerService;

/**
 * This is class is dedicated to sending messages give by another service to its
 * client. This is done by simply checking if the list of messages to be sent in
 * the cache is empty or not. If the cache is empty, then do nothing, otherwise
 * work with the client's message receiver.
 * 
 * @author Estrellado, Vandolf
 * 
 */
public class SSCServerMessageSender implements SSCProtocol {

	private SSCServerService service;
	private SSCCrypto crypt;
	private boolean isWorking;
	private List<byte[]> pending;

	/**
	 * Initialize the protocol as well as the crypto.
	 * 
	 * @param service
	 *            the service that launched this protocol.
	 * @param crypt
	 *            the crypto that will be used.
	 */
	public SSCServerMessageSender(SSCServerService service, SSCCrypto crypt) {
		this.service = service;
		this.crypt = crypt;
		isWorking = true;
		pending = new ArrayList<byte[]>();
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
	 * Sends the messages in the buffer to the client and then removes the sent
	 * message in the pending cache.
	 */
	@Override
	public void performMagic() {
		boolean debug;
		try {
			if (pending.size() > 0) {
				debug = service.getServerClass().getSettings().debugSenderProtocol;

				if (debug)
					System.out.println(service.getClient().getName()
							+ " Sender: forwarding E(m)");
				// send E(m)
				SSCStreamManager.sendBytes(service.getClient()
						.getOutputStream(), crypt.encrypt(pending.get(0)));

				if (debug)
					System.out.println(service.getClient().getName()
							+ "Sender: waiting for client E(confirmCode)");
				// wait for client E(confirmCode)
				byte[] resultCode = crypt.decrypt(SSCStreamManager
						.readBytes(service.getClient().getInputStream()));

				boolean confirmed = true;
				for (int i = 0; i < resultCode.length; i++)
					if (resultCode[i] != crypt.getConfirmCode()[i]) {
						confirmed = false;
						break;
					}

				if (confirmed) {
					if (debug)
						System.out.println(service.getClient().getName()
								+ "Sender: sending H(m)");
					// send H(m)
					SSCStreamManager.sendBytes(
							service.getClient().getOutputStream(),
							MessageDigest.getInstance("SHA-1").digest(
									pending.get(0)));
				} else
					return;// something went wrong - do not send the message
				pending.remove(0);
			}
		} catch (IOException | NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	public void addToPending(byte[] m) {
		pending.add(m);
	}

}
