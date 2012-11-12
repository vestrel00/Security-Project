package com.vestrel00.ssc.server.protocols;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

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

	/*
	 * Note: streams connections are closed by the service.
	 * Not the protocol.
	 */
	private BufferedReader in;
	private PrintWriter out;
	private String inStr;
	private boolean isWorking;

	/**
	 * Initialize the protocol.
	 */
	public SSCProtocol(BufferedReader in, PrintWriter out) {
		this.in = in;
		this.out = out;
		isWorking = true;
	}

	/**
	 * Tells the protocol to keep handling requests by the client. If the client
	 * quits, this returns false, flagging the service to stop.
	 */
	public boolean work() {
		try {
			if (isWorking && (inStr = in.readLine()) != null)
				processStr();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * This is where the magic of the protocol happens.<br>
	 * Process the string received from the client.<br>
	 * The string may be a password, username, service request, or anything that
	 * this protocol can offer.
	 */
	private void processStr() {
		// TODO
		if (inStr.contentEquals("quit"))
			isWorking = false;
		else
			System.out.println(inStr);
	}

}
