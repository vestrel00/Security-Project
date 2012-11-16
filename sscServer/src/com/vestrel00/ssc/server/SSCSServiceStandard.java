package com.vestrel00.ssc.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.SecureRandom;

import com.vestrel00.ssc.server.datatypes.SSCBufferClient;
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
	private Socket client;
	private SSCServerService otherClientService;
	private DataInputStream in;
	private DataOutputStream out;
	private boolean inService, isInChat;
	private String clientName, otherClientName;
	private SSCBufferClient clientBuffer;
	private SecureRandom rand;

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
	public SSCSServiceStandard(SSCServer serverClass, Socket client) {
		this.serverClass = serverClass;
		this.client = client;
	}

	/**
	 * Allocate buffer space in server, open IO connections, and initialize
	 * other variables.
	 */
	private void init() {
		clientBuffer = serverClass.getBuffer().allocate(10);
		inService = true;
		rand = new SecureRandom();
		openIO();
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
		SSCStreamManager.sendBytes(out,
				"Login or Create new account? (login | create)".getBytes());
		String choice = new String(SSCStreamManager.readBytes(in));
		if (choice.contentEquals("login")) {
			while (attempts < 3) {
				SSCStreamManager.sendBytes(out, "Enter username".getBytes());
				String uname = new String(SSCStreamManager.readBytes(in));
				SSCStreamManager.sendBytes(out, "Enter password".getBytes());
				String pass = new String(SSCStreamManager.readBytes(in));
				if (true) {// if(database.authenticate(uname, pass))
					if (serverClass.clientIsOnline(uname)) {
						attempts++;
						SSCStreamManager.sendBytes(out, "bad".getBytes());
					} else {
						clientName = uname;
						SSCStreamManager.sendBytes(out, "good".getBytes());
						return true;
					}
				} else {
					SSCStreamManager.sendBytes(out, "bad".getBytes());
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

	/**
	 * Initialize the in/out streams that will be used for communicating with
	 * the client. Closes the in and out streams and creates new ones if streams
	 * already exist.
	 * 
	 * @throws IOException
	 */
	private void openIO() {
		if (out == null) {
			try {
				out = new DataOutputStream(client.getOutputStream());
				in = new DataInputStream(client.getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			closeIO();
			openIO();
		}
	}

	/**
	 * Closes the in and out streams.
	 * 
	 * @throws IOException
	 */
	private void closeIO() {
		if (out != null) {
			try {
				out.close();
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			out = null;
			in = null;
		}
	}

	@Override
	public void stopService() {
		if (inService) {
			inService = false;
			closeIO();
			serverClass.removeService(clientName, clientBuffer.getBufferId());
			try {
				if (client != null)
					client.close();
				client = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void run() {
		init();
		try {
			login();
			connect();
		} catch (IOException e) {
			stopService();
		}

		// isOutputShutdown checks if client is still connected
		while (inService && client.isOutputShutdown()) {
			if (sender != null && !sender.work())
				stopService();
		}
	}

	/**
	 * <p>
	 * Launch the receiver thread that will listen for incoming client messages.
	 * This opens up a new socket with the client's initSender() in order to get
	 * a separate in and out streams from this sender.
	 * </p>
	 * 
	 */
	private void initReceiver() {
		receiver = new SSCServerMessageReceiver(this, client, crypt);
		// serverClass.get
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
			String uname = new String(SSCStreamManager.readBytes(in));
			if (uname.contentEquals(clientName))
				SSCStreamManager.sendBytes(out, "nonsense".getBytes());
			else if (serverClass.clientIsOnline(uname)) {
				otherClientService = serverClass
						.getServiceByName(otherClientName);
				if (!otherClientService.isInChat()) {
					SSCStreamManager.sendBytes(out, "online".getBytes());
					otherClientName = new String(uname);
					retry = false;
				}
			} else
				SSCStreamManager.sendBytes(out, "unavailable".getBytes());
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
		isInChat = true;
		if (!otherClientService.isInChat()) {
			// this service got here first - compute keys!
			// generate secretKey
			secretKey = new byte[16];
			rand.nextBytes(secretKey);
			// generate the confirmCode
			confirmCode = new byte[4];
			rand.nextBytes(confirmCode);
			// set the keys for the other service in wait
			otherClientService.setSecretKey(secretKey);
			otherClientService.setConfirmCode(confirmCode);
		} else { // this got here after the other service already computed keys
			// wait for the other service to send over secret key
			while (secretKey == null && confirmCode == null)
				continue;
		}
		// both services have the keys ready to send to their clients
		SSCStreamManager.sendBytes(out, secretKey);
		SSCStreamManager.readBytes(in); // wait for client ok
		SSCStreamManager.sendBytes(out, confirmCode);
		crypt = new SSCCryptoAES(secretKey, confirmCode);
		sender = new SSCServerMessageSender(this, crypt);
	}

	public SSCServer getServerClass() {
		return serverClass;
	}

	@Override
	public DataInputStream getInputStream() {
		return in;
	}

	@Override
	public DataOutputStream getOutputStream() {
		return out;
	}

	@Override
	public SSCServerService getOtherClientService() {
		return otherClientService;
	}

	@Override
	public String getClientName() {
		return clientName;
	}

	@Override
	public boolean isInChat() {
		return isInChat;
	}

	@Override
	public SSCBufferClient getClientBuffer() {
		return clientBuffer;
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

}
