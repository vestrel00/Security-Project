package com.vestrel00.ssc.client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.vestrel00.ssc.client.interf.SSCCryptoPrivate;
import com.vestrel00.ssc.client.interf.SSCProtocol;
import com.vestrel00.ssc.client.protocols.SSCClientMessageSender;
import com.vestrel00.ssc.client.protocols.SSCClientMessageReceiver;
import com.vestrel00.ssc.client.shared.SSCByteMethods;
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
	private SSCCryptoPrivate crypt;
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
		userIn.close();
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
					// flag service to perform login protocol
					SSCStreamManager.sendBytes(out, choice.getBytes());
					// wait for the OK
					SSCStreamManager.readBytes(in);
					System.out.println("Enter username");

					boolean retry = true;
					try {
						while (retry) {
							// send username
							username = userIn.readLine();
							SSCStreamManager
									.sendBytes(out, username.getBytes());
							// wait for the salt (this is a bogus value if
							// given username does not exist)
							byte[] salt = SSCStreamManager.readBytes(in);
							System.out.println("Enter password");
							// send saltedHashedPassword
							try {
								SSCStreamManager.sendBytes(
										out,
										MessageDigest.getInstance("SHA-1")
												.digest(SSCByteMethods.concat(
														salt, userIn.readLine()
																.getBytes())));
							} catch (NoSuchAlgorithmException e) {
								e.printStackTrace();
							}
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
						System.out.println("Unable to login.\n"
								+ "User may not exist or is already be online "
								+ "or name and/or password is incorrect.");
						attempts++;
					}
				}
				return false;
			} else if (choice.contentEquals("create")) {
				// Flag the service to perform the createAccount Protocol
				SSCStreamManager.sendBytes(out, "create".getBytes());
				createAccount();
				return login();
			} else if (choice != null)
				System.out.println("Unknown command : " + choice);
		}
	}

	/**
	 * Perform the create protocol with the service.
	 * 
	 */
	// TODO Wrap with RSA
	private void createAccount() throws IOException {
		boolean retry = true;
		try {
			while (retry) {
				System.out.println("Enter username");
				// Send username
				SSCStreamManager.sendBytes(out, userIn.readLine().getBytes());
				// Wait for server response
				String response = new String(SSCStreamManager.readBytes(in));
				if (response.contentEquals("bad")) {
					System.out.println("Username is already taken.");
					continue;
				}
				System.out.println("Enter password");
				// Send password
				SSCStreamManager.sendBytes(out, userIn.readLine().getBytes());
				// wait for server response
				SSCStreamManager.readBytes(in);
				System.out.println("Account created successfully.");
				retry = false;
			}
		} catch (IndexOutOfBoundsException e) {
			System.out.println("Invalid input.");
			SSCStreamManager.sendBytes(out, "restart".getBytes());
			createAccount();
		}
	}

	/**
	 * Initialize the protocols and starts listening for SERVER input and user
	 * input.
	 * 
	 */
	public void start() throws IOException, NoSuchAlgorithmException {
		try {
			if (!login()) {
				System.out.println("Failed to login. Exiting system...");
				finish();
			}
			option();
		} catch (IOException | IndexOutOfBoundsException e) {
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
		// infinite loop. Exit option within loop
		while (true) {
			System.out.println("What do you want to do?");
			System.out
					.println("(checkFriendsList | checkInvites | sendInvites | connect | exit)");
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
				// this thread will remain listening for incoming service inputs
				while (isRunning) {
					if (!receiver.work())
						finish();
				}
			} else if (choice.contentEquals("exit")) {
				System.out.println("Goodbye.");
				finish();
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

	/**
	 * Perform the protocol with the server.
	 * 
	 * @throws IOException
	 */
	private void sendInvites() throws IOException {
		// wait for user input
		System.out
				.println("Enter the username you wish to invite to your friend list.");
		String name = userIn.readLine();
		SSCStreamManager.sendBytes(out, name.getBytes());
		// wait for resultCode (0 = added, 1 = already friends, 2 = invite sent,
		// 3 = already sent invite, -1 = not exist/blocked
		int resultCode = Integer.parseInt(new String(SSCStreamManager
				.readBytes(in)));
		switch (resultCode) {
		case 0:
			System.out.println(name + " has been sent");
			break;
		case 1:

			break;
		default:
			System.out.println(name
					+ " does not exist or does not want to be your friend.");
			break;
		}

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
		// System.out.println(username + " generating new socket");
		Socket sock = new Socket(host, port);
		DataInputStream receptionIn = new DataInputStream(sock.getInputStream());
		DataOutputStream receptionOut = new DataOutputStream(
				sock.getOutputStream());

		// Note: 2 different in/out streams at this point! Do not get confused.
		// send username : will generate a pending client in the server list
		SSCStreamManager.sendBytes(receptionOut, username.getBytes());
		// System.out.println(username + " waiting for server OK");
		// wait for server reception OK
		SSCStreamManager.readBytes(receptionIn);
		// server should now have a new pending client in the pending list
		// System.out.println(username + " waiting for service OK");
		// wait for the OK from the service's initReceiver.
		SSCStreamManager.readBytes(receptionIn);

		// System.out.println(username + " launching sender thread");
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

	public SSCClientMessageSender getSender() {
		return sender;
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
