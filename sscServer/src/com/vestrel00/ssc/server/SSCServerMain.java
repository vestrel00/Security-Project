package com.vestrel00.ssc.server;

import java.io.IOException;

/**
 * Starts the server program of SSC.
 * @author Estrellado, Vandolf
 *
 */
public class SSCServerMain {
	
	public static Thread thread;
	public static SSCServer server;

	public static void main(String args[]) throws IOException{
		server = new SSCServer(8080);
		thread = new Thread(server);
		thread.start();
	}
	
}
