package com.vestrel00.ssc.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import com.vestrel00.ssc.server.datatypes.SSCBufferClient;
import com.vestrel00.ssc.server.interf.SSCServer;
import com.vestrel00.ssc.server.interf.SSCServerService;
import com.vestrel00.ssc.server.interf.SSCProtocol;
import com.vestrel00.ssc.server.protocols.SSCServerProtocol;
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

	private SSCProtocol protocol;
	private SSCServer serverClass;
	private Socket client;
	private SSCServerService otherClientService;
	private DataInputStream in;
	private DataOutputStream out;
	private boolean inService, isInChat;
	private String clientName, otherClientName;
	private SSCBufferClient clientBuffer;

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
					if (serverClass.clientIsOnline(uname, false)) {
						attempts++;
						SSCStreamManager.sendBytes(out, "bad".getBytes());
						continue;
					}
					clientName = uname;
					SSCStreamManager.sendBytes(out, "good".getBytes());
					return true;
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

	public void stopService() {
		inService = false;
		closeIO();
		serverClass.removeService(clientName, clientBuffer.getBufferId());
		// not necessary since this is running on the same thread but..
		if (protocol != null)
			protocol.stopWorking();
		try {
			if (client != null)
				client.close();
			client = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		init();
		try {
			login();
			connect();
		} catch (IOException e) {
			e.printStackTrace();
		}

		while (inService && client.isOutputShutdown()) {
			if (protocol != null && !protocol.work())
				inService = false;
		}
		stopService();
	}

	/**
	 * login succeeded now connect to another client that is also logged in the
	 * server. Perform the client-client connection protocol with the server
	 * service. <b>Server computes both the private key and confirm code</b>
	 * 
	 * @throws IOException
	 */
	// TODO replace with actual implementation
	private void connect() throws IOException {
		boolean retry = true;
		while (retry) {
			// wait for requested client's username
			String uname = new String(SSCStreamManager.readBytes(in));
			if (uname.contentEquals(clientName))
				SSCStreamManager.sendBytes(out, "nonsense".getBytes());
			else if (serverClass.clientIsOnline(uname, true)) {
				SSCStreamManager.sendBytes(out, "online".getBytes());
				otherClientName = new String(uname);
				otherClientService = serverClass
						.getServiceByName(otherClientName);
				retry = false;
			} else
				SSCStreamManager.sendBytes(out, "unavailable".getBytes());
		}

		retry = true;
		while (retry) {
			// the requested client also requests this service's client
			if (otherClientService.getOtherClientName().contentEquals(
					clientName)) {
				retry = false;
				// TODO init protocol and send same params used to client
			}
		}
		isInChat = true;
	}

	public SSCServer getServerClass() {
		return serverClass;
	}

	@Override
	public void forwardMessageToService(SSCServerService service, byte[] em,
			byte[] hm) {
		// TODO
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
	public String getOtherClientName() {
		return otherClientName;
	}

	@Override
	public boolean isInChat() {
		return isInChat;
	}

	@Override
	public SSCBufferClient getClientBuffer() {
		return clientBuffer;
	}

}
