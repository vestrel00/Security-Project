package com.vestrel00.ssc.server;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import com.vestrel00.ssc.server.datatypes.SSCPendingClient;
import com.vestrel00.ssc.server.datatypes.SSCServerClient;
import com.vestrel00.ssc.server.interf.SSCCryptoPrivate;
import com.vestrel00.ssc.server.interf.SSCServer;
import com.vestrel00.ssc.server.interf.SSCServerService;
import com.vestrel00.ssc.server.protocols.SSCServerMessageReceiver;
import com.vestrel00.ssc.server.protocols.SSCServerMessageSender;
import com.vestrel00.ssc.server.shared.SSCByteMethods;
import com.vestrel00.ssc.server.shared.SSCCryptoAES;
import com.vestrel00.ssc.server.shared.SSCStreamManager;
import com.vestrel00.ssc.server.shared.SSCStringMethods;

/**
 * An implementation of the SSCServerService that uses RSA to secure initial
 * client-server connection.
 * 
 * @author Estrellado, Vandolf
 * @see SSCServerService
 * 
 */
public class SSCSServiceStandard implements SSCServerService {

	private static final String comma = ",";
	private SSCServerMessageSender sender;
	private SSCServerMessageReceiver receiver;
	private SSCCryptoPrivate crypt;
	private SSCServer serverClass;
	private SSCServerService clientPartnerService;
	private boolean inService, isConnected, isInChat;
	private SecureRandom rand;
	private SSCServerClient client;
	private SSCStringMethods methods;

	/**
	 * Need the following to be field members so other service objects have
	 * access to them.
	 */
	private byte[] secretKey, confirmCode;

	/**
	 * Create the service.
	 * 
	 * @param serverClass
	 *            The server which launched this service.
	 * @param client
	 *            The client that will be serviced.
	 */
	public SSCSServiceStandard(SSCServer serverClass, SSCServerClient client) {
		this.serverClass = serverClass;
		this.client = client;
		inService = true;
		rand = new SecureRandom();
		methods = new SSCStringMethods();
	}

	/**
	 * Uses RSA to secure login or creation of new account. Will end the service
	 * if password does not match with username three times.
	 * <ol>
	 * <li>Send public key</li>
	 * <li>TODO</li>
	 * </ol>
	 * 
	 * @throws IOException
	 */
	// TODO wrap with RSA
	public boolean login() throws IOException {
		int attempts = 0;
		SSCStreamManager.sendBytes(client.getOutputStream(),
				"Login or Create new account? (login | create)".getBytes());
		String choice = new String(SSCStreamManager.readBytes(client
				.getInputStream()));
		if (choice.contentEquals("login")) {
			while (attempts < 3) {
				// messy patch
				if (attempts > 0)
					// need to read in choice - refer to client login protocol
					SSCStreamManager.readBytes(client.getInputStream());

				SSCStreamManager.sendBytes(client.getOutputStream(),
						"OK".getBytes());
				// wait for the username
				String uname = new String(SSCStreamManager.readBytes(client
						.getInputStream()));
				// send the salt or a bogus one
				if (SSCServerDB.userExists(uname))
					SSCStreamManager.sendBytes(client.getOutputStream(),
							SSCServerDB.getSalt(uname));
				else {
					byte[] bogusSalt = new byte[8];
					rand.nextBytes(bogusSalt);
					SSCStreamManager.sendBytes(client.getOutputStream(),
							bogusSalt);
				}
				// wait for the saltedHashedPass
				byte[] saltedHashedPass = SSCStreamManager.readBytes(client
						.getInputStream());

				if (SSCServerDB.userExists(uname)) {
					if (SSCByteMethods.equal(saltedHashedPass,
							SSCServerDB.getSaltedHashedPass(uname))) {
						if (serverClass.clientIsOnline(uname)) {
							attempts++;
							SSCStreamManager.sendBytes(
									client.getOutputStream(), "bad".getBytes());
						} else {
							client.setName(uname);
							SSCStreamManager
									.sendBytes(client.getOutputStream(),
											"good".getBytes());
							System.out.println("User " + uname
									+ " has logged in.");
							return true;
						}
					} else {
						SSCStreamManager.sendBytes(client.getOutputStream(),
								"bad".getBytes());
						attempts++;
					}
				} else {
					SSCStreamManager.sendBytes(client.getOutputStream(),
							"bad".getBytes());
					attempts++;
				}
			}
			return false;
		} else if (choice.contentEquals("create")) {
			createAccount();
			return login();
		} else
			return false;

	}

	/**
	 * Perform the create protocol with the client.
	 * 
	 * @throws IOException
	 * 
	 */
	// TODO Wrap with RSA
	private void createAccount() throws IOException {
		boolean retry = true;
		while (retry) {
			// wait for the username
			String uname = new String(SSCStreamManager.readBytes(client
					.getInputStream()));
			if (SSCServerDB.userExists(uname)) {
				SSCStreamManager.sendBytes(client.getOutputStream(),
						"bad".getBytes());
				continue;
			}
			SSCStreamManager.sendBytes(client.getOutputStream(),
					"good".getBytes());
			// wait for the password
			byte[] pass = SSCStreamManager.readBytes(client.getInputStream());
			// user requests to start over
			if (new String(pass).contentEquals("restart")) {
				System.out.flush();
				continue;
			}

			SSCStreamManager.sendBytes(client.getOutputStream(),
					"good".getBytes());
			// create the user
			byte[] saltB = new byte[8];
			rand.nextBytes(saltB);
			try {
				SSCServerDB.createUser(
						uname,
						saltB,
						MessageDigest.getInstance("SHA-1").digest(
								SSCByteMethods.concat(saltB, pass)));
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
			retry = false;
		}
	}

	@Override
	public void stopService(boolean remove) {
		if (inService) {
			if (isInChat) {
				isInChat = false;
				inService = false;
				clientPartnerService.stopService(true);
			}
			inService = false;
			client.closeIO();
			if (receiver != null)
				receiver.closeIO();
			if (remove)
				serverClass.removeService(client.getName(), client.getBuffer()
						.getBufferId());
			try {
				client.getSocket().close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void run() {
		try {
			// TODO Once a client connects, it is added onto the service
			// list, however with name = null. So if more than one client is
			// connected but has not logged in, how can we tell which client
			// to remove if their name is null? Answer is to make name as an
			// integer if client is connected but not logged in instead of
			// null.
			login();
			option();
		} catch (IOException e) {
			stopService(true);
		}
	}

	/**
	 * Wait for the client to make a choice about what to do next.
	 * 
	 * @throws IOException
	 */
	// TODO wrap with RSA
	private void option() throws IOException {
		String choice;
		// infinite loop. Exit within loop.
		while (true) {
			choice = new String(SSCStreamManager.readBytes(client
					.getInputStream()));

			if (choice.contentEquals("checkFriendsList"))
				checkFriends();
			else if (choice.contentEquals("checkInvites"))
				checkInvites();
			else if (choice.contentEquals("sendInvites"))
				sendInvites();
			else if (choice.contentEquals("connect")) {
				connect();
				// isOutputShutdown checks if client is still able to write us
				while (inService && !client.getSocket().isOutputShutdown()) {
					if (sender != null && !sender.work())
						stopService(true);
				}
			} else if (choice.contentEquals("exit")) {
				stopService(true);
			}
		}
	}

	private void checkFriends() {
		// TODO Auto-generated method stub

	}

	private void checkInvites() {
		// TODO Auto-generated method stub

	}

	/**
	 * 
	 * Perform the protocol with the client
	 * 
	 * @throws IOException
	 */
	private void sendInvites() throws IOException {
		// wait for name
		String name = new String(SSCStreamManager.readBytes(client
				.getInputStream()));
		// check db if name exist
		if (!SSCServerDB.userExists(name)) {
			SSCStreamManager.sendBytes(client.getOutputStream(),
					"-1".getBytes());
			return;
		}

		// check db if our client is already friends with the client with the
		// given name
		String friendList = SSCServerDB.getFriendList(client.getName());
		if (methods.isInList(friendList, name)) {
			SSCStreamManager
					.sendBytes(client.getOutputStream(), "1".getBytes());
			return;
		}

		// check if our client already has sent an invite
		String sentList = SSCServerDB.getSentInvites(client.getName());
		if (methods.isInList(sentList, name)) {
			SSCStreamManager
					.sendBytes(client.getOutputStream(), "3".getBytes());
			return;
		}

		// check db if user with given name is blocking our client
		String blockList = SSCServerDB.getBlockList(name);
		if (methods.isInList(blockList, client.getName())) {
			SSCStreamManager
					.sendBytes(client.getOutputStream(), "-1".getBytes());
			return;
		}

		// TODO make update -> remove
		// check if the other client also sent an invite to our client and
		// its invite is in our client's received invites
		String receivedInvites = SSCServerDB.getReceivedInvites(client
				.getName());
		if (methods.isInList(receivedInvites, name)) {
			receivedInvites = methods.removeFromList(receivedInvites, name);
			// remove our client's received invite
			SSCServerDB
					.updateReceivedInvites(client.getName(), receivedInvites);
			// remove the sender's invite
			String otherSentInvites = methods.removeFromList(
					SSCServerDB.getSentInvites(name), client.getName());
			SSCServerDB.updateSentInvites(name, otherSentInvites);
			// add to friends list for both clients
			SSCServerDB.insertFriend(client, newFriend)
			
			SSCStreamManager
					.sendBytes(client.getOutputStream(), "0".getBytes());
		} else {
			
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
		boolean retry = true;
		while (retry) {
			// wait for requested client's username
			client.setPartnerName(new String(SSCStreamManager.readBytes(client
					.getInputStream())));
			if (client.getPartnerName().contentEquals(client.getName()))
				SSCStreamManager.sendBytes(client.getOutputStream(),
						"nonsense".getBytes());
			else if (serverClass.clientIsOnline(client.getPartnerName())) {
				clientPartnerService = serverClass.getServiceByName(client
						.getPartnerName());
				if (!clientPartnerService.isInChat()
						&& (!clientPartnerService.isConnected() || clientPartnerService
								.getClient().getPartnerName()
								.contentEquals(client.getName()))) {
					SSCStreamManager.sendBytes(client.getOutputStream(),
							"online".getBytes());
					retry = false;
				} else {
					SSCStreamManager.sendBytes(client.getOutputStream(),
							"unavailable".getBytes());
				}
			} else
				SSCStreamManager.sendBytes(client.getOutputStream(),
						"unavailable".getBytes());
		}
		initSender();
		initReceiver();
	}

	/**
	 * <p>
	 * Need to decide which service will compute the session keys. So whoever
	 * gets here first is the one!
	 * </p>
	 * The service that gets here first compute the secret key and confirm code
	 * that is passed onto the protocol which passes onto the crypto.
	 * 
	 * @throws IOException
	 */
	// TODO wrap with RSA
	private void initSender() throws IOException {
		isConnected = true;
		if (!clientPartnerService.isConnected()) {
			// this service got here first - compute keys!
			// generate secretKey
			secretKey = new byte[16];
			rand.nextBytes(secretKey);
			// generate the confirmCode
			confirmCode = new byte[4];
			rand.nextBytes(confirmCode);
			// set the keys for the other service
			clientPartnerService.setSecretKey(secretKey);
			clientPartnerService.setConfirmCode(confirmCode);
		} else { // this got here after the other service already computed keys
			// wait for the other service to send over secret key
			while (secretKey == null && confirmCode == null) {
				continue;
			}
		}
		// both services have the keys ready to send to their clients
		SSCStreamManager.sendBytes(client.getOutputStream(), secretKey);
		// wait for client OK
		SSCStreamManager.readBytes(client.getInputStream());
		SSCStreamManager.sendBytes(client.getOutputStream(), confirmCode);
		crypt = new SSCCryptoAES(secretKey, confirmCode);
		sender = new SSCServerMessageSender(this, crypt);
	}

	/**
	 * <p>
	 * Launch the receiver thread that will listen for incoming client messages.
	 * We then wait for the client to create another socket to get a new pair of
	 * in/out streams. Proceed when the pending client is in the server pending
	 * list.
	 * </p>
	 * 
	 * @throws IOException
	 * 
	 */
	// TODO wrap with RSA
	private void initReceiver() throws IOException {
		// retrieve the client from the pending list
		// blocks until client is in pending list
		SSCPendingClient pc = serverClass.retrievePendingClient(client
				.getName());
		// client and server now has new socket pairs to use! send OK.
		SSCStreamManager.sendBytes(pc.getOutputStream(), "OK".getBytes());

		// finally init and launch the receiver
		receiver = new SSCServerMessageReceiver(this, pc.getSocket(), crypt);
		new Thread(receiver).start();
	}

	public SSCServer getServerClass() {
		return serverClass;
	}

	@Override
	public SSCServerClient getClient() {
		return client;
	}

	@Override
	public SSCServerService getClientPartnerService() {
		return clientPartnerService;
	}

	@Override
	public boolean isConnected() {
		return isConnected;
	}

	@Override
	public void setSecretKey(byte[] secretKey) {
		this.secretKey = secretKey;
	}

	@Override
	public void setConfirmCode(byte[] confirmCode) {
		this.confirmCode = confirmCode;
	}

	@Override
	public SSCServerMessageReceiver getReceiver() {
		return receiver;
	}

	@Override
	public SSCServerMessageSender getSender() {
		return sender;
	}

	@Override
	public boolean isInChat() {
		return isInChat;
	}

	@Override
	public void setOnChat(boolean isInChat) {
		this.isInChat = isInChat;
	}

}
