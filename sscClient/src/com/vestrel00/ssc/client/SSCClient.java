package com.vestrel00.ssc.client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

import com.vestrel00.ssc.client.interf.SSCCrypto;

/**
 * The client that interacts with the server.
 * 
 * @author Estrellado, Vandolf
 * 
 */
public class SSCClient {

	private Socket socket;
	private DataInputStream in;
	private BufferedReader userIn;
	private DataOutputStream out;
	private String userStr;
	private SSCCrypto crypt;

	public SSCClient(String host, int port) throws UnknownHostException,
			IOException {
		socket = new Socket(host, port);
		initIOTools();
		try {
			crypt = new SSCCryptoAES("0123456789abcdef".getBytes());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}

	/**
	 * <p>
	 * Initialize the BufferedReader(s) and PrintWriter that will be used for
	 * communicating with the client. Closes the in and out streams and creates
	 * new ones if streams already exist.
	 * </p>
	 * <p>
	 * This should have more than 1 reader for streaming in user input and
	 * socket input.
	 * </p>
	 * 
	 * @throws IOException
	 */
	public void initIOTools() throws IOException {
		if (out == null) {
			out = new DataOutputStream(socket.getOutputStream());
			in = new DataInputStream(socket.getInputStream());
			userIn = new BufferedReader(new InputStreamReader(System.in));
		} else {
			closeIO();
			initIOTools();
		}
	}

	/**
	 * Closes the in and out streams.
	 * 
	 * @throws IOException
	 */
	public void closeIO() throws IOException {
		out.close();
		in.close();
		userIn.close();
		out = null;
		in = null;
		userIn = null;
	}

	/**
	 * Starts listening for user input. User input is streamed out to the server
	 * service.
	 * 
	 * @throws IOException
	 */
	public void start() throws IOException {
		// TODO
		while ((userStr = userIn.readLine()) != null) {
			SSCStreamManager.sendBytes(out, crypt.encrypt(userStr.getBytes()));
		}
		finish();
	}

	/**
	 * Finish the client program, closing all the streams.
	 * 
	 * @throws IOException
	 */
	public void finish() throws IOException {
		closeIO();
		socket.close();
	}

}
