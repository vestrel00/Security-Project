package com.vestrel00.ssc.client;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * The client that interacts with the server.
 * @author Estrellado, Vandolf
 *
 */
public class SSCClient {

	private Socket socket;
	
	public SSCClient(String host, int port) throws UnknownHostException, IOException{
		socket = new Socket(host, port);
	}

}
