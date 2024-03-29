package com.vestrel00.ssc.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;

/**
 * Main class that runs the SSCClient program.
 * 
 * @author Estrellado, Vandolf
 * 
 */
public class SSCClientMain {

	private static SSCClient client;
	private static BufferedReader in;
	private static String host;
	private static int port, bufferSize;

	public static void main(String args[]) throws IOException,
			NoSuchAlgorithmException {
		in = new BufferedReader(new InputStreamReader(System.in));
		// calling in.close closes the System.in stream. Don't do it.
		System.out.println("Hello non-shady person!");
		System.out.println("This is Vandolf Estrellado's 480 project.");
		enterHostName();
		enterPort();
		enterBufferSize();
		start();
	}

	private static void start() throws NoSuchAlgorithmException, IOException {
		try {
			System.out.println("Connecting to " + host + ":" + port);
			System.out.println("Please wait...");
			client = new SSCClient(host, port, bufferSize);
			System.out.println("Connection success.");
			client.start();
		} catch (IOException e) {
			System.out.println("Cannot find server running on host " + host
					+ ":" + port +".");
			enterHostName();
			enterPort();
			start();
		}
	}

	private static void enterHostName() throws IOException {
		System.out.println("Enter the host name on which a server is running");
		host = in.readLine();
	}

	private static void enterPort() throws IOException {
		try {
			System.out
					.println("Enter the port number on which the server is listening");
			port = Integer.parseInt(in.readLine());
		} catch (NumberFormatException e) {
			System.out.println("Invalid number format.");
			enterPort();
		}
	}

	private static void enterBufferSize() throws IOException {
		try {
			System.out
					.println("Enter the buffer size you want the server to allocate");
			bufferSize = Integer.parseInt(in.readLine());
		} catch (NumberFormatException e) {
			System.out.println("Invalid number format.");
			enterBufferSize();
		}
	}

}
