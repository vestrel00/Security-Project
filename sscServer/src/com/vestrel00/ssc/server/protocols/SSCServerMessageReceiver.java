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
		boolean debug;

		// Need to call System.out.flush() for some reason... WHY??? TODO WHYY??
		// Everything goes to hell if flush isn't called...
		// BUG ON THE JDK?????? TODO FIND OUT!
		while (service.getClientPartnerService().getReceiver() == null) {
			System.out.flush(); // a random fix! like seriously wtf?
			continue;
		}

		// flag our client that the other client is ready
		try {
			SSCStreamManager.sendBytes(out, "OK".getBytes());
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// set the service to be in chat
		service.setOnChat(true);

		try {
			while (!socket.isOutputShutdown()) {
				debug = service.getServerClass().getSettings().debugReceiverProtocol;
				if (debug)
					System.out.println(service.getClient().getName()
							+ "Receiver: waiting for E(m)");
				// Wait for E(m)
				byte[] em = SSCStreamManager.readBytes(in);

				if (debug)
					System.out.println(service.getClient().getName()
							+ "Receiver: sending E(confirmCode)");
				// send E(confirmCode)
				SSCStreamManager.sendBytes(out,
						crypt.encrypt(crypt.getConfirmCode()));

				if (debug)
					System.out.println(service.getClient().getName()
							+ "Receiver: waiting for H(m)");
				// Wait for H(m)
				byte[] hm = SSCStreamManager.readBytes(in);
				// m
				byte[] m = crypt.decrypt(em);
				// H(E(m))
				byte[] hem = MessageDigest.getInstance("SHA-1").digest(m);
				// authenticate
				if (hem.length != hm.length)
					return;

				boolean confirmed = true;
				for (int index = 0; index < hm.length; index++) {
					if (hm[index] != hem[index]) { // E(m) was tampered with
						confirmed = false;
						break;
					}
				}

				if (confirmed) {
					if (debug)
						System.out.println(service.getClient().getName()
								+ "Receiver: everything checked out");
					// Everything checked out
					service.getClient().getBuffer().addMessage(m);
					service.getClientPartnerService().getSender()
							.addToPending(m);
				} else {
					System.out.println("Connection from "
							+ service.getClient().getName() + " and "
							+ service.getClient().getPartnerName()
							+ " may be compromised.");
				}
			}
		} catch (IOException e) {
			service.stopService(true);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		closeIO();

	}

	/**
	 * Close all i/o streams including the socket itself.
	 */
	public void closeIO() {
		try {
			out.close();
			in.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
