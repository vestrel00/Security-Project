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
	private String host, username, partnerName;
	private int port;

	/**
	 * Constructor.
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
		init(maxBufferSize);
	}

	/**
	 * <p>
	 * Sends the requested buffer size to the server. This is performed with
	 * server's ConnectionReception.
	 * </p>
	 * <ol>
	 * <li>Send server null flagging the server that this is the initial
	 * connection</li>
	 * <li>Wait for server OK.</li>
	 * <li>Send the bufferSize</li>
	 * </ol>
	 * 
	 * @throws IOException
	 */
	private void init(int maxBufferSize) throws IOException {
		// TODO prevent using null as user name
		SSCStreamManager.sendBytes(out, "null".getBytes());
		SSCStreamManager.readBytes(in);
		SSCStreamManager.sendBytes(out, String.valueOf(maxBufferSize)
				.getBytes());
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
		out = new DataOutputStream(socket.getOutputStream());
		in = new DataInputStream(socket.getInputStream());
		userIn = new BufferedReader(new InputStreamReader(System.in));
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

					boolean retry = true;
					try {
						while (retry) {
							// send username
							username = userIn.readLine();
							SSCStreamManager
									.sendBytes(out, username.getBytes());
							System.out.println(new String(SSCStreamManager
									.readBytes(in)));
							// send password
							SSCStreamManager.sendBytes(out, userIn.readLine()
									.getBytes());
							retry = false;
						}
					} catch (IndexOutOfBoundsException e) {
						retry = true;
					}
					if (new String(SSCStreamManager.readBytes(in))
							.contentEquals("good")) {
						System.out.println("Login successful");
						return true;
					} else {
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
		try {
			option();
		} catch (IOException | IndexOutOfBoundsException e) {
			finish();
		}
		
		// this thread will remain listening for incoming service inputs
		while (isRunning) {
			if (!receiver.work())
				finish();
		}
	}

	/**
	 * Choose whether to configure settings, check which friends are online,
	 * check invites, send invites, or connect with a friend that is online.
	 * This runs with the option protocol of the server service.
	 */
	// TODO wrap with RSA
	private void option() throws IOException {
		String choice;
		boolean retry = true;
		while (retry) {
			System.out.println("What do you want to do?");
			System.out
					.println("(checkFriendsList | checkInvites | sendInvites | connect)");
			choice = userIn.readLine();
			SSCStreamManager.sendBytes(out, choice.getBytes());
			if (choice.contentEquals("checkFriendsList"))
				checkFriends();
			else if (choice.contentEquals("checkInvites"))
				checkInvites();
			else if (choice.contentEquals("sendInvites"))
				sendInvites();
			else if (choice.contentEquals("connect")) {
				connect();
				retry = false;
			} else
				System.out.println("Unknown command " + choice);
		}
	}

	private void checkFriends() {
		// TODO Auto-generated method stub

	}

	private void checkInvites() {
		// TODO Auto-generated method stub

	}

	private void sendInvites() {
		// TODO Auto-generated method stub

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
		while (!isInChat) {
			System.out
					.println("Enter username of client you wish to chat with");
			partnerName = new String(userIn.readLine());
			SSCStreamManager.sendBytes(out, partnerName.getBytes());
			String response = new String(SSCStreamManager.readBytes(in));
			if (response.contentEquals("online")) {
				System.out.println(partnerName + " is online.\nWaiting for "
						+ partnerName + "...");
				// both clients are go, initialize the receiver and sender
				initReceiver();
				initSender();
				isInChat = true;
			} else if (response.contentEquals("nonsense"))
				System.out.println("You are " + partnerName
						+ ". You may not chat with yourself.");
			else
				System.out
						.println(partnerName
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
	 * Launch the sender thread that handles user input and sends those input to
	 * the service's receiver which is being launched at the same time as this.
	 * This opens up new input and output streams.
	 * 
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	// TODO wrap with RSA
	private void initSender() throws UnknownHostException, IOException {
		Socket sock = new Socket(host, port);
		DataInputStream receptionIn = new DataInputStream(sock.getInputStream());
		DataOutputStream receptionOut = new DataOutputStream(
				sock.getOutputStream());

		// Note: 2 different in/out streams at this point! Do not get confused.
		// send username : will generate a pending client in the server list
		SSCStreamManager.sendBytes(receptionOut, username.getBytes());
		// wait for server reception OK
		SSCStreamManager.readBytes(receptionIn);
		// server should now have a new pending client in the pending list
		// wait for the OK from the service's initReceiver.
		SSCStreamManager.readBytes(receptionIn);

		// finally init and launch the sender
		sender = new SSCClientMessageSender(this, sock, crypt);
		new Thread(sender).start();
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
		// can close sender's IO here but readLine is still blocking
		// doesn't matter either way- small thing.
		// Note - tried writing to System.out
		isRunning = false;
		System.out.println("You have been logged out");
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

	public boolean isRunning() {
		return isRunning;
	}

	public String getUserName() {
		return username;
	}

	public String getPartnerName() {
		return partnerName;
	}

}
