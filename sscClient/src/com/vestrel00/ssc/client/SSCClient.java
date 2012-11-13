package com.vestrel00.ssc.client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;

import com.vestrel00.ssc.client.interf.SSCProtocol;
import com.vestrel00.ssc.client.protocols.SSCClientProtocol;
import com.vestrel00.ssc.client.protocols.SSCUserIS;
import com.vestrel00.ssc.client.shared.SSCStreamManager;

/**
 * The client that interacts with the server.
 * 
 * @author Estrellado, Vandolf
 * 
 */
public class SSCClient {

	/**
	 * User input stream is running on a separate thread;
	 */
	private BufferedReader userIn;
	private DataInputStream in;
	private DataOutputStream out;
	private Socket socket;
	private SSCClientBuffer buffer;
	private SSCProtocol protocol;
	private SSCUserIS userIStream;
	private boolean isRunning;

	/**
	 * Constructor. Immediately invokes the login protocol.
	 */
	public SSCClient(String host, int port, int maxBufferSize)
			throws UnknownHostException, IOException {
		socket = new Socket(host, port);
		buffer = new SSCClientBuffer(maxBufferSize);
		isRunning = true;
		initIOTools();
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
	 * Perform the login protocol with the server service using RSA.
	 * 
	 * @return true if success.
	 * @throws IOException
	 */
	// TODO replace with actual implementation
	private boolean login() throws IOException {
		int attempts = 0;
		System.out.println(new String(SSCStreamManager.readBytes(in)));
		SSCStreamManager.sendBytes(out, userIn.readLine().getBytes());
		System.out.println(new String(SSCStreamManager.readBytes(in)));
		SSCStreamManager.sendBytes(out, userIn.readLine().getBytes());
		while (attempts < 3) {
			System.out.println(new String(SSCStreamManager.readBytes(in)));
			SSCStreamManager.sendBytes(out, userIn.readLine().getBytes());
			if (new String(SSCStreamManager.readBytes(in))
					.contentEquals("good"))
				return true;
			else
				attempts++;
		}
		return false;
	}

	/**
	 * Initialize the protocols and starts listening for SERVER input and user
	 * input.
	 * 
	 */
	public void start() throws IOException, NoSuchAlgorithmException {
		if (!login()) {
			System.out.println("Failed to login. Exiting system...");
			System.exit(1);
		}
		// login succeeded now connect to another client that is also logged in
		// the server. Perform the client-client connection protocol with the
		// server service- TODO
		protocol = new SSCClientProtocol(this, "0123456789abcdef",
				"kkjf9934ihssj");

		// now connected with another user
		// launch the user input thread
		userIStream = new SSCUserIS(this);
		new Thread(userIStream).start();
		// this thread will remain listening for input from the other user
		while (isRunning) {
			if (!protocol.work())
				isRunning = false;
		}
		finish();
	}

	/**
	 * Finish the client program, closing all the streams and stopping the
	 * protocol.
	 * 
	 * @throws IOException
	 */
	public void finish() throws IOException {
		closeIO();
		socket.close();
		// not really necessary since this has no effect
		protocol.stopWorking();
	}

	public BufferedReader getUserInputStream() {
		return userIn;
	}

	public SSCClientBuffer getBuffer() {
		return buffer;
	}

	public DataOutputStream getOutputStream() {
		return out;
	}

	public DataInputStream getServerInputStream() {
		return in;
	}

	public SSCProtocol getClientProtocol() {
		return protocol;
	}

}
