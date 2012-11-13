package com.vestrel00.ssc.client;

import java.io.IOException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;

/**
 * Main class that runs the SSCClient program.
 * 
 * @author Estrellado, Vandolf
 * 
 */
public class SSCClientMain {

	public static SSCClient client;

	public static void main(String args[]) throws UnknownHostException,
			IOException, NoSuchAlgorithmException{
		client = new SSCClient("127.0.0.1", 8080, 10);
		client.start();
	}

}
