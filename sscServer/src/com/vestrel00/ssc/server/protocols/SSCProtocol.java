package com.vestrel00.ssc.server.protocols;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.vestrel00.ssc.server.shared.SSCCryptoAES;
import com.vestrel00.ssc.server.shared.SSCStreamManager;
import com.vestrel00.ssc.server.interf.SSCCrypto;
import com.vestrel00.ssc.server.interf.SSCServerService;

/**
 * <p>
 * This is the protocol that the server runs for each client.
 * </p>
 * <ol>
 * <li>Client enters server information.</li>
 * <li>Client performs the handshake protocol with the server or registers with
 * the server if first time.</li>
 * <li>Client chooses messaging protocol.</li>
 * <li>//TODO</li>
 * </ol>
 * 
 * @author Estrellado, Vandolf
 * 
 */
public class SSCProtocol {

	/**
	 * Note: streams connections are closed by the service. Not the protocol.
	 */
	private DataInputStream in;
	private DataOutputStream out;
	private byte[] inData;
	private SSCServerService service;
	private SSCCrypto crypt;
	private boolean isWorking;

	/**
	 * Initialize the protocol.
	 */
	public SSCProtocol(SSCServerService service, DataInputStream in,
			DataOutputStream out) {
		this.service = service;
		this.in = in;
		this.out = out;
		isWorking = true;
		try {
			// TODO parameterize
			crypt = new SSCCryptoAES("0123456789abcdef".getBytes(), "kkjf9934ihssj");
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Tells the protocol to keep handling requests by the client. If the client
	 * quits, this returns false, flagging the service to stop.
	 */
	public boolean work() {
		if (isWorking) {
			processData();
			return true;
		} else
			return false;
	}

	/**
	 * This is where the magic of the protocol happens.<br>
	 * Process the message received from the client.<br>
	 * The string may be a password, username, service request, or anything that
	 * this protocol can offer. This may end the service/connection if the
	 * client closes the program abruptly.<br>
	 * For this case, we receive E(m) and H(m).
	 * <ol>
	 * <li>decrypt E(m)</li>
	 * <li>hash m and check if it equals the received H(m)</li>
	 * <li>if it checks out, store the data in the buffer</li>
	 * </ol>
	 */
	private void processData() {
		try {
			inData = SSCStreamManager.readBytes(in);
			// TODO it works, now use the buffer to store this!
			System.out.println(new String(crypt.decrypt(inData)));
		} catch (IOException e) {
			isWorking = false;
		}
	}

}
