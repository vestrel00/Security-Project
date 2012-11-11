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
	
	public static Socket socket;
	
	public static void main(String args[]) throws UnknownHostException, IOException{
		socket = new Socket("127.0.0.1", 8080);
	}
	
}
