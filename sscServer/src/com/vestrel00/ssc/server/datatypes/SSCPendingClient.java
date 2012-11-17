package com.vestrel00.ssc.server.datatypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * A simple data type that pairs the accepted sockets with client names.
 * 
 * @author Estrellado, Vandolf
 * 
 */
public class SSCPendingClient {

	protected String name;
	protected Socket socket;
	protected DataInputStream in;
	protected DataOutputStream out;

	public SSCPendingClient(String name, Socket socket) {
		this.name = name;
		this.socket = socket;
		openIO();
	}

	/**
	 * Initialize the in/out streams that will be used for communicating with
	 * the client. Closes the in and out streams and creates new ones if streams
	 * already exist.
	 * 
	 * @throws IOException
	 */
	public void openIO() {
		try {
			out = new DataOutputStream(socket.getOutputStream());
			in = new DataInputStream(socket.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Closes the in and out streams.
	 * 
	 * @throws IOException
	 */
	public void closeIO() {
		try {
			out.close();
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public DataInputStream getInputStream() {
		return in;
	}

	public DataOutputStream getOutputStream() {
		return out;
	}

	public Socket getSocket() {
		return socket;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
