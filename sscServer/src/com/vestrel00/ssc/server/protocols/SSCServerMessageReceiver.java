package com.vestrel00.ssc.server.protocols;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.vestrel00.ssc.server.shared.SSCStreamManager;
import com.vestrel00.ssc.server.interf.SSCCrypto;
import com.vestrel00.ssc.server.interf.SSCServerService;

public class SSCServerMessageReceiver implements Runnable {

	private SSCServerService service;
	private SSCCrypto crypt;
	private DataOutputStream out;
	private DataInputStream in;
	private Socket socket;

	public SSCServerMessageReceiver(SSCServerService service, Socket socket,
			SSCCrypto crypt) {
		this.service = service;
		this.socket = socket;
		this.crypt = crypt;
		try {
			out = new DataOutputStream(socket.getOutputStream());
			in = new DataInputStream(socket.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Wait for E(m) and H(m) from the client. Authenticate and if everything
	 * checks out, store m in the client's buffer and forward E(m) and H(m) to
	 * the service which handles the destination client.
	 */
	@Override
	public void run() {
		try {
			while (!socket.isOutputShutdown()) {
				// Wait for E(m)
				byte[] em = SSCStreamManager.readBytes(in);
				// tell client that it has been received
				SSCStreamManager.sendBytes(out,
						crypt.encrypt(crypt.getConfirmCode()));
				// Wait for H(m)
				byte[] hm = SSCStreamManager
						.readBytes(service.getInputStream());
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
				service.getOtherClientService().getSender().addToPending(m);

			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		// clean up
		try {
			out.close();
			in.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
