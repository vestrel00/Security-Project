package com.vestrel00.ssc.server;

import java.io.IOException;
import java.security.SecureRandom;

import com.vestrel00.ssc.server.datatypes.SSCPendingClient;
import com.vestrel00.ssc.server.datatypes.SSCServerClient;
import com.vestrel00.ssc.server.interf.SSCCrypto;
import com.vestrel00.ssc.server.interf.SSCServer;
import com.vestrel00.ssc.server.interf.SSCServerService;
import com.vestrel00.ssc.server.protocols.SSCServerMessageReceiver;
import com.vestrel00.ssc.server.protocols.SSCServerMessageSender;
import com.vestrel00.ssc.server.shared.SSCCryptoAES;
import com.vestrel00.ssc.server.shared.SSCStreamManager;

/**
 * An implementation of the SSCServerService that uses RSA to secure initial
 * client-server connection.
 * 
 * @author Estrellado, Vandolf
 * @see SSCServerService
 * 
 */
public class SSCSServiceStandard implements SSCServerService {

	private SSCServerMessageSender sender;
	private SSCServerMessageReceiver receiver;
	private SSCCrypto crypt;
	private SSCServer serverClass;
	private SSCServerService clientPartnerService;
	private boolean inService, isConnected, isInChat;
	private SecureRandom rand;
	private SSCServerClient client;

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
	// TODO replace with actual implementation!
	public boolean login() throws IOException {
		int attempts = 0;
		SSCStreamManager.sendBytes(client.getOutputStream(),
				"Login or Create new account? (login | create)".getBytes());
		String choice = new String(SSCStreamManager.readBytes(client
				.getInputStream()));
		if (choice.contentEquals("login")) {
			while (attempts < 3) {
				SSCStreamManager.sendBytes(client.getOutputStream(),
						"Enter username".getBytes());
				String uname = new String(SSCStreamManager.readBytes(client
						.getInputStream()));
				SSCStreamManager.sendBytes(client.getOutputStream(),
						"Enter password".getBytes());
				String pass = new String(SSCStreamManager.readBytes(client
						.getInputStream()));
				if (true) {// if(database.authenticate(uname, pass))
					if (serverClass.clientIsOnline(uname)) {
						attempts++;
						SSCStreamManager.sendBytes(client.getOutputStream(),
								"bad".getBytes());
					} else {
						client.setName(uname);
						SSCStreamManager.sendBytes(client.getOutputStream(),
								"good".getBytes());
						System.out.println("User " + uname + " has logged in.");
						return true;
					}
				} else {
					SSCStreamManager.sendBytes(client.getOutputStream(),
							"bad".getBytes());
					attempts++;
				}
			}
			return false;
		} else if (choice.contentEquals("create"))
			return createAccount();
		else
			return false;

	}

	private boolean createAccount() {
		// TODO store in database H(m) and the salt
		return true;
	}

	@Override
	public void stopService(boolean remove) {
		if (inService) {
			if (isInChat) {
				isInChat = false;
				String lo = client.getName();
				lo += " has logged out.";
				clientPartnerService.getSender().addToPending(lo.getBytes());
				// wait 1 second before stopping the other service for the above
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
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
			login();
			option();
		} catch (IOException e) {
			stopService(true);
		}

		// isOutputShutdown checks if client is still able to write to us
		while (inService && !client.getSocket().isOutputShutdown()) {
			if (sender != null && !sender.work())
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
		boolean retry = true;
		while (retry) {
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
				retry = false;
			}
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
				if (!clientPartnerService.isInChat()) {
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
			// set the keys for the other service in wait
			clientPartnerService.setSecretKey(secretKey);
			clientPartnerService.setConfirmCode(confirmCode);
		} else { // this got here after the other service already computed keys
			// wait for the other service to send over secret key
			while (secretKey == null && confirmCode == null)
				continue;
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
