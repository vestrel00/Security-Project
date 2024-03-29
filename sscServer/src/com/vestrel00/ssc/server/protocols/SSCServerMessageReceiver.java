package com.vestrel00.ssc.server.protocols;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.vestrel00.ssc.server.shared.SSCStreamManager;
import com.vestrel00.ssc.server.interf.SSCCryptoPrivate;
import com.vestrel00.ssc.server.interf.SSCServerService;

/**
 * Listens for incoming messages from the client program.
 * 
 * @author Estrellado, Vandolf
 * 
 */
public class SSCServerMessageReceiver implements Runnable {

	private SSCServerService service;
	private SSCCryptoPrivate crypt;
	private DataOutputStream out;
	private DataInputStream in;
	private Socket socket;

	public SSCServerMessageReceiver(SSCServerService service, Socket socket,
			SSCCryptoPrivate crypt) {
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
		// or end this client's service if partner client
		try {
			if (service.getClientPartnerService().getClient().getPartnerName()
					.contentEquals(service.getClient().getName())) {
				SSCStreamManager.sendBytes(out, "OK".getBytes());
				// set the service to be in chat
				service.setOnChat(true);
			} else {
				SSCStreamManager.sendBytes(out, "NO".getBytes());
				service.stopService(true);
				closeIO();
				return;
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		try {
			while (!socket.isOutputShutdown()) {
				debug = service.getServerClass().getSettings().debugReceiverProtocol;

				if (debug)
					System.out.println(service.getClient().getName()
							+ "Receiver: waiting for em");
				// wait for em
				byte[] em = SSCStreamManager.readBytes(in);

				if (debug)
					System.out.println(service.getClient().getName()
							+ "Receiver: sending confirmCode");
				// send confirmCode
				SSCStreamManager.sendBytes(out, crypt.getConfirmCode());

				if (debug)
					System.out.println(service.getClient().getName()
							+ "Receiver: waiting for IV");
				// wait for IV (initialization vector used for decryption)
				byte[] iv = SSCStreamManager.readBytes(in);

				if (debug)
					System.out.println(service.getClient().getName()
							+ "Receiver: sending confirmCode");
				// send confirmCode
				SSCStreamManager.sendBytes(out, crypt.getConfirmCode());

				if (debug)
					System.out.println(service.getClient().getName()
							+ "Receiver: waiting for H(m)");
				// Wait for H(E(m))
				byte[] hm = SSCStreamManager.readBytes(in);
				// m
				byte[] m = crypt.decrypt(em, iv);
				// H(E(m))
				byte[] hem = MessageDigest.getInstance("SHA-1").digest(em);
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
					// make sure hm and iv are stored before em!
					service.getClientPartnerService().getSender()
							.addToPendingHEM(hm);
					service.getClientPartnerService().getSender()
							.addToPendingIV(iv);
					service.getClientPartnerService().getSender()
							.addToPendingEM(em);
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
