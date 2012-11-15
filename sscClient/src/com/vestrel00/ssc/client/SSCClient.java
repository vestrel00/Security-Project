package com.vestrel00.ssc.client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;

import com.vestrel00.ssc.client.interf.SSCCrypto;
import com.vestrel00.ssc.client.interf.SSCProtocol;
import com.vestrel00.ssc.client.protocols.SSCClientMessageSender;
import com.vestrel00.ssc.client.protocols.SSCClientMessageReceiver;
import com.vestrel00.ssc.client.shared.SSCCryptoAES;
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
	private DataOutputStream out;
	private DataInputStream in;
	private Socket socket;
	private SSCClientBuffer buffer;
	private SSCProtocol receiver;
	private SSCClientMessageSender sender;
	private SSCCrypto crypt;
	private boolean isRunning, isInChat;
	private String host;
	private int port;

	/**
	 * Constructor. Immediately invokes the login protocol.
	 */
	public SSCClient(String host, int port, int maxBufferSize)
			throws UnknownHostException, IOException {
		this.host = host;
		this.port = port;
		socket = new Socket(host, port);
		buffer = new SSCClientBuffer(maxBufferSize);
		isRunning = true;
		isInChat = false;
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
		if (userIn != null)
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

		// now featuring an infinite loop!
		while (true) {
			String choice = userIn.readLine();
			if (choice.contentEquals("login")) {
				while (attempts < 3) {
					SSCStreamManager.sendBytes(out, choice.getBytes());
					System.out.println(new String(SSCStreamManager
							.readBytes(in)));
					SSCStreamManager.sendBytes(out, userIn.readLine()
							.getBytes());
					System.out.println(new String(SSCStreamManager
							.readBytes(in)));
					SSCStreamManager.sendBytes(out, userIn.readLine()
							.getBytes());
					if (new String(SSCStreamManager.readBytes(in))
							.contentEquals("good"))
						return true;
					else {
						System.out
								.println("Unable to login.\n"
										+ "User may already be online or name and/or password is incorrect.");
						attempts++;
					}
				}
				return false;
			} else if (choice.contentEquals("create"))
				return createAccount();
			else if (choice != null)
				System.out.println("Unknown command : " + choice);
		}
	}

	private boolean createAccount() throws IOException {
		// TODO Auto-generated method stub
		SSCStreamManager.sendBytes(out, "create".getBytes());
		return true;
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
		connect();
		// the userInputStream will no longer be used in this thread
		userIn.close();
		// this thread will remain listening for incoming service inputs
		while (isRunning) {
			if (!receiver.work())
				isRunning = false;
		}
		finish();
	}

	/**
	 * Launch the sender thread that handles user input and sends those input to
	 * the service's receiver which is being launched at the same time as this.
	 * This opens up a new socket with the service's initReceiver() in order to
	 * get a separate in and out streams from this receiver.
	 * 
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	private void initSender() throws UnknownHostException, IOException {
		// TODO PORT PORT
		sender = new SSCClientMessageSender(this, new Socket(host, port), crypt);
		new Thread(sender).start();
	}

	/**
	 * login succeeded now connect to another client that is also logged in the
	 * server. Perform the client-client connection protocol with the server
	 * service. <b>Server computes both the private key and confirm code</b>
	 * 
	 * @throws IOException
	 */
	// TODO wrap with RSA
	private void connect() throws IOException {
		System.out.println("Login successful");
		while (!isInChat) {
			System.out
					.println("Enter username of client you wish to chat with");
			String clientName = new String(userIn.readLine());
			SSCStreamManager.sendBytes(out, clientName.getBytes());
			String response = new String(SSCStreamManager.readBytes(in));
			if (response.contentEquals("online")) {
				System.out.println(clientName
						+ " is online.\nWaiting for other user...");
				// both clients are go, initialize the receiver and sender
				initReceiver();
				initSender();
				isInChat = true;
			} else if (response.contentEquals("nonsense"))
				System.out.println("You are " + clientName
						+ ". You may not chat with yourself.");
			else
				System.out
						.println(clientName
								+ " is offline or chatting with someone else or does not exist.");
		}
	}

	/**
	 * Initialize the protocol which also initializes the crypto.
	 * 
	 * @throws IOException
	 */
	// TODO wrap with RSA
	private void initReceiver() throws IOException {
		// wait for secretKey
		byte[] secretKey = SSCStreamManager.readBytes(in);
		// send OK to server
		SSCStreamManager.sendBytes(out, "ok".getBytes());
		// wait for confirmCode
		byte[] confirmCode = SSCStreamManager.readBytes(in);
		crypt = new SSCCryptoAES(secretKey, confirmCode);
		receiver = new SSCClientMessageReceiver(this, crypt);
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
		receiver.stopWorking();
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

	public SSCProtocol getReceiver() {
		return receiver;
	}

}
