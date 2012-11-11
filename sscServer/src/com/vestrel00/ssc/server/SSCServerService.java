package com.vestrel00.ssc.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.vestrel00.ssc.server.protocols.SSCProtocol;

/**
 * The server's response to a client request is this service.
 * @author Estrellado, Vandolf
 *
 */
public class SSCServerService implements Runnable {

	private SSCProtocol protocol;
	private Socket client;
	private BufferedReader in;
	private PrintWriter out;
	private boolean inService;

	/**
	 * Initialize the runnable giving its full attention to the given client.
	 * 
	 * @throws IOException
	 */
	public SSCServerService(Socket client) throws IOException {
		this.client = client;
		inService = true;
		protocol = new SSCProtocol();
		initIOTools();
	}

	/**
	 * Initialize the BufferedReader and PrintWriters that will be used for
	 * communicating with the client.
	 * 
	 * @throws IOException
	 */
	private void initIOTools() throws IOException {
		out = new PrintWriter(client.getOutputStream(), false);
		in = new BufferedReader(new InputStreamReader(client.getInputStream()));
	}

	/**
	 * Close all the streaming connections including the client socket.
	 * 
	 * @throws IOException
	 */
	public void stopService() throws IOException {
		inService = false;
		out.close();
		in.close();
		client.close();
	}

	@Override
	public void run() {
		while (inService) {
			protocol.work();
		}
		try {
			stopService();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
