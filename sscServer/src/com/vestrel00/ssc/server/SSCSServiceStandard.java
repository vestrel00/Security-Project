package com.vestrel00.ssc.server;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import com.vestrel00.ssc.server.datatypes.SSCPendingClient;
import com.vestrel00.ssc.server.datatypes.SSCServerClient;
import com.vestrel00.ssc.server.interf.SSCCryptoPrivate;
import com.vestrel00.ssc.server.interf.SSCCryptoPublic;
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
 * client-server connection.<br>
 * 
 * <ol>
 * <b>To amend</b>
 * <li>sending list of names as plaintext</li>
 * <li>sending the RSA public key on pre-login on SSCServerStandard#run()</li>
 * <li>sending the AES private key on connect()</li>
 * </ol>
 * 
 * @author Estrellado, Vandolf
 * @see SSCServerService
 * 
 */
public class SSCSServiceStandard implements SSCServerService {

	private SSCServerMessageSender sender;
	private SSCServerMessageReceiver receiver;
	private SSCCryptoPrivate privCrypt;
	private SSCCryptoPublic pubCrypt;
	private SSCServer serverClass;
	private SSCServerService clientPartnerService;
	private boolean inService, isConnected, isInChat;
	private SecureRandom rand;
	private SSCServerClient client;
	private SSCStringMethods methods;
	private StringBuilder builder;

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
	public SSCSServiceStandard(SSCServer serverClass, SSCServerClient client,
			SSCCryptoPublic pubCrypt) {
		this.serverClass = serverClass;
		this.client = client;
		this.pubCrypt = pubCrypt;
		inService = true;
		rand = new SecureRandom();
		methods = new SSCStringMethods();
		builder = new StringBuilder();
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
		String choice = new String(pubCrypt.decrypt(SSCStreamManager
				.readBytes(client.getInputStream())));
		if (choice.contentEquals("login")) {
			while (attempts < 3) {
				// messy patch
				if (attempts > 0)
					// need to read in choice - refer to client login protocol
					SSCStreamManager.readBytes(client.getInputStream());

				SSCStreamManager.sendBytes(client.getOutputStream(),
						"OK".getBytes());
				// wait for the E(username)
				String uname = new String(pubCrypt.decrypt(SSCStreamManager
						.readBytes(client.getInputStream())));
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
				// wait for the E(saltedHashedPass)
				byte[] saltedHashedPass = pubCrypt.decrypt(SSCStreamManager
						.readBytes(client.getInputStream()));

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
	private void createAccount() throws IOException {
		boolean retry = true;
		while (retry) {
			// wait for the (username)
			String uname = new String(pubCrypt.decrypt(SSCStreamManager
					.readBytes(client.getInputStream())));
			if (SSCServerDB.userExists(uname)) {
				SSCStreamManager.sendBytes(client.getOutputStream(),
						"bad".getBytes());
				continue;
			}
			SSCStreamManager.sendBytes(client.getOutputStream(),
					"good".getBytes());
			// wait for the E(password)
			byte[] pass = pubCrypt.decrypt(SSCStreamManager.readBytes(client
					.getInputStream()));
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
	private void option() throws IOException {
		String choice;
		// infinite loop. Exit within loop.
		while (true) {
			// wait for E(choice)
			choice = new String(pubCrypt.decrypt(SSCStreamManager
					.readBytes(client.getInputStream())));

			if (choice.contentEquals("friends"))
				try {
					SSCStreamManager.sendBytes(client.getOutputStream(),
							SSCServerDB.getFriendList(client.getName())
									.getBytes());
				} catch (IndexOutOfBoundsException e) {
					SSCStreamManager.sendBytes(client.getOutputStream(),
							"You have no friends =(".getBytes());
				}
			else if (choice.contentEquals("enemies"))
				try {
					SSCStreamManager.sendBytes(client.getOutputStream(),
							SSCServerDB.getEnemyList(client.getName())
									.getBytes());
				} catch (IndexOutOfBoundsException e) {
					SSCStreamManager.sendBytes(client.getOutputStream(),
							"You have no enemies =)".getBytes());
				}
			else if (choice.contentEquals("sent"))
				try {
					SSCStreamManager.sendBytes(client.getOutputStream(),
							SSCServerDB.getSentInvites(client.getName())
									.getBytes());
				} catch (IndexOutOfBoundsException e) {
					SSCStreamManager.sendBytes(client.getOutputStream(),
							"You do not want to be friends with anyone =("
									.getBytes());
				}
			else if (choice.contentEquals("received"))
				try {
					SSCStreamManager.sendBytes(client.getOutputStream(),
							SSCServerDB.getReceivedInvites(client.getName())
									.getBytes());
				} catch (IndexOutOfBoundsException e) {
					SSCStreamManager
							.sendBytes(client.getOutputStream(),
									"No one wants to be friends with you =("
											.getBytes());
				}
			else if (choice.contentEquals("send"))
				sendInvites();
			else if (choice.contentEquals("block"))
				blockUser();
			else if (choice.contentEquals("accept"))
				acceptInvite();
			else if (choice.contentEquals("reject"))
				rejectInvite();
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

	/**
	 * Perform the blockUser protocol with the client. Note that the user does
	 * not have to exist yet so this will simply add the given name to the list!
	 * Also note that this removes the friend with the given name if exist.
	 * 
	 * @throws IOException
	 */
	private void blockUser() throws IOException {
		// wait for E(name)
		String name = new String(pubCrypt.decrypt(SSCStreamManager
				.readBytes(client.getInputStream())));
		// insert name in block list
		// note that the insertion method does not add duplicate names.
		SSCServerDB.insertEnemy(client.getName(), name);
		// remove from the friends list
		SSCServerDB.removeFromFriends(builder, client.getName(), name);
	}

	/**
	 * Perform the acceptInvite protocol with the client
	 * 
	 * @throws IOException
	 */
	private void acceptInvite() throws IOException {
		// send received invites list
		try {
			SSCStreamManager.sendBytes(client.getOutputStream(), SSCServerDB
					.getReceivedInvites(client.getName()).getBytes());
			// throw bounds exception here if empty list
			// skipping the following lines
			// wait for E(response)
			String response = new String(pubCrypt.decrypt(SSCStreamManager
					.readBytes(client.getInputStream())));
			if (!response.contentEquals("fail")) {
				// delete the received invite
				SSCServerDB.removeFromReceivedInvites(builder,
						client.getName(), response);
				// delete the sent invite from the other client
				SSCServerDB.removeFromSentInvites(builder, response,
						client.getName());
				// add both as friends
				SSCServerDB.insertFriend(client.getName(), response);
				SSCServerDB.insertFriend(response, client.getName());
			}
		} catch (IndexOutOfBoundsException e) {
			SSCStreamManager.sendBytes(client.getOutputStream(),
					"empty".getBytes());
		}

	}

	/**
	 * Perform the rejectInvite protocol with the client
	 * 
	 * @throws IOException
	 */
	private void rejectInvite() throws IOException {
		// send received invites list
		try {
			SSCStreamManager.sendBytes(client.getOutputStream(), SSCServerDB
					.getReceivedInvites(client.getName()).getBytes());
			// throw bounds exception here if empty list
			// skipping the following lines
			// wait for E(response)
			String response = new String(pubCrypt.decrypt(SSCStreamManager
					.readBytes(client.getInputStream())));
			if (!response.contentEquals("fail"))
				// delete the received invite
				SSCServerDB.removeFromReceivedInvites(builder,
						client.getName(), response);
			// delete the sent invite from the other client
			SSCServerDB.removeFromSentInvites(builder, response,
					client.getName());
		} catch (IndexOutOfBoundsException e) {
			SSCStreamManager.sendBytes(client.getOutputStream(),
					"empty".getBytes());
		}
	}

	/**
	 * 
	 * Perform the protocol with the client. Note that if our client is blocking
	 * the given name, that enemy will be removed and an invite will be sent.
	 * 
	 * @throws IOException
	 */
	private void sendInvites() throws IOException {
		// wait for E(name)
		String name = new String(pubCrypt.decrypt(SSCStreamManager
				.readBytes(client.getInputStream())));
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
		String blockList = SSCServerDB.getEnemyList(name);
		if (methods.isInList(blockList, client.getName())) {
			SSCStreamManager.sendBytes(client.getOutputStream(),
					"-1".getBytes());
			return;
		}

		// case that the other client has already sent our client an invite
		// remove our client's received invite (if exist)
		// remove the sender's invite (if exist)
		if (SSCServerDB.removeFromReceivedInvites(builder, client.getName(),
				name)
				&& SSCServerDB.removeFromSentInvites(builder, name,
						client.getName())) {
			// remove our client's block listed enemy if exist
			SSCServerDB.removeFromEnemies(builder, client.getName(), name);
			// add to friends list for both clients
			SSCServerDB.insertFriend(client.getName(), name);
			SSCServerDB.insertFriend(name, client.getName());
			SSCStreamManager
					.sendBytes(client.getOutputStream(), "0".getBytes());
		} else {
			// case that the other client has NOT sent our client an invite
			// invites do no exist so create them
			SSCServerDB.insertReceivedInvite(name, client.getName());
			SSCServerDB.insertSentInvite(client.getName(), name);
			SSCStreamManager
					.sendBytes(client.getOutputStream(), "2".getBytes());
		}
	}

	/**
	 * login succeeded now connect to another client that is also logged in the
	 * server. Perform the client-client connection protocol with the server
	 * service. <b>Server computes both the private key and confirm code</b>
	 * 
	 * @throws IOException
	 */
	private void connect() throws IOException {
		boolean retry = true;
		while (retry) {
			// wait for E(requested client's username)
			client.setPartnerName(new String(pubCrypt.decrypt(SSCStreamManager
					.readBytes(client.getInputStream()))));
			// client wants to talk to himself
			if (client.getPartnerName().contentEquals(client.getName()))
				SSCStreamManager.sendBytes(client.getOutputStream(),
						"nonsense".getBytes());
			// check if the partner client is online
			else if (serverClass.clientIsOnline(client.getPartnerName())) {
				// check if client is friends with the partner client
				String lst = SSCServerDB.getFriendList(client.getName());
				if (!lst.contains(client.getPartnerName())) {
					SSCStreamManager.sendBytes(client.getOutputStream(),
							"unavailable".getBytes());
					continue;
				}

				clientPartnerService = serverClass.getServiceByName(client
						.getPartnerName());
				if (!clientPartnerService.isInChat()
						&& (!clientPartnerService.isConnected() || clientPartnerService
								.getClient().getPartnerName()
								.contentEquals(client.getName()))) {
					SSCStreamManager.sendBytes(client.getOutputStream(),
							"online".getBytes());
					retry = false;
				} else
					SSCStreamManager.sendBytes(client.getOutputStream(),
							"unavailable".getBytes());
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
	 * that is passed onto the protocol which passes onto the privCrypto.
	 * 
	 * @throws IOException
	 */
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
		// TODO should not send secret key in the clear
		SSCStreamManager.sendBytes(client.getOutputStream(), secretKey);
		// wait for client OK
		SSCStreamManager.readBytes(client.getInputStream());
		SSCStreamManager.sendBytes(client.getOutputStream(), confirmCode);
		privCrypt = new SSCCryptoAES(secretKey, confirmCode);
		sender = new SSCServerMessageSender(this, privCrypt);
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
		receiver = new SSCServerMessageReceiver(this, pc.getSocket(), privCrypt);
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
