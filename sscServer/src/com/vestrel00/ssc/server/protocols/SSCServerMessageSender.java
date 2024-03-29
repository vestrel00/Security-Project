package com.vestrel00.ssc.server.protocols;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.vestrel00.ssc.server.shared.SSCStreamManager;
import com.vestrel00.ssc.server.interf.SSCCryptoPrivate;
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
	private SSCCryptoPrivate crypt;
	private boolean isWorking;
	private List<byte[]> pendingEM, pendingHEM, pendingIV;

	/**
	 * Initialize the protocol as well as the crypto.
	 * 
	 * @param service
	 *            the service that launched this protocol.
	 * @param crypt
	 *            the crypto that will be used.
	 */
	public SSCServerMessageSender(SSCServerService service,
			SSCCryptoPrivate crypt) {
		this.service = service;
		this.crypt = crypt;
		isWorking = true;
		pendingEM = new ArrayList<byte[]>();
		pendingHEM = new ArrayList<byte[]>();
		pendingIV = new ArrayList<byte[]>();
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
			if (pendingEM.size() > 0) {
				debug = service.getServerClass().getSettings().debugSenderProtocol;

				if (debug)
					System.out.println(service.getClient().getName()
							+ " Sender: forwarding E(m)");
				// send E(m)
				SSCStreamManager.sendBytes(service.getClient()
						.getOutputStream(), pendingEM.get(0));

				// wait for client confirmCode
				byte[] resultCode = SSCStreamManager.readBytes(service
						.getClient().getInputStream());

				if (debug)
					System.out.println(service.getClient().getName()
							+ " Sender: forwarding IV)");
				// send IV
				SSCStreamManager.sendBytes(service.getClient()
						.getOutputStream(), pendingIV.get(0));

				if (debug)
					System.out.println(service.getClient().getName()
							+ "Sender: waiting for client E(confirmCode)");
				// wait for client confirmCode
				resultCode = SSCStreamManager.readBytes(service.getClient()
						.getInputStream());

				boolean confirmed = true;
				for (int i = 0; i < resultCode.length; i++)
					if (resultCode[i] != crypt.getConfirmCode()[i]) {
						confirmed = false;
						break;
					}

				if (confirmed) {
					if (debug)
						System.out.println(service.getClient().getName()
								+ "Sender: sending H(E(m))");
					// send H(E(m))
					SSCStreamManager.sendBytes(service.getClient()
							.getOutputStream(), pendingHEM.get(0));
				} else
					return;// something went wrong - do not send the message
				pendingEM.remove(0);
				pendingHEM.remove(0);
				pendingIV.remove(0);
			}
		} catch (IOException e) {
			// Do nothing ?
		}
	}

	public void addToPendingEM(byte[] em) {
		pendingEM.add(em);
	}

	public void addToPendingHEM(byte[] hm) {
		pendingHEM.add(hm);
	}

	public void addToPendingIV(byte[] iv) {
		pendingIV.add(iv);
	}

}
