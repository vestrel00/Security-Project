package com.vestrel00.ssc.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Random;

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
	private SSCServerService destinationService;
	private DataInputStream in;
	private DataOutputStream out;
	private boolean inService;
	private int serviceId, serverBufferId;
	private Random rand;

	/**
	 * Create the service asking the client to login or create new account.
	 * 
	 * @param serverClass
	 *            The server which launched this service.
	 * @param client
	 *            The client that will be serviced.
	 * @throws IOException
	 */
	public SSCSServiceStandard(SSCServer serverClass, Socket client)
			throws IOException {
		this.serverClass = serverClass;
		this.client = client;
		serverBufferId = serverClass.getBuffer().allocate(10);
		inService = true;
		rand = new Random();
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
				"Login or Create new account?".getBytes());
		String choice = new String(SSCStreamManager.readBytes(in));
		if (choice.contentEquals("login")) {
			SSCStreamManager.sendBytes(out, "Enter username".getBytes());
			String uname = new String(SSCStreamManager.readBytes(in));
			while (attempts < 3) {
				SSCStreamManager.sendBytes(out, "Enter password".getBytes());
				String pass = new String(SSCStreamManager.readBytes(in));
				if (true) {// if(database.authenticate(uname, pass))
					SSCStreamManager.sendBytes(out, "good".getBytes());
					return true;
				} else {
					SSCStreamManager.sendBytes(out, "bad".getBytes());
					attempts++;
				}
			}
			return false;
		} else if (choice.contentEquals("create"))
			return create();
		else
			return false;

	}

	private boolean create() {
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
	public void initProtocol(int destServiceId, String secretKey,
			String confirmCode) {
		protocol = new SSCServerProtocol(this, secretKey, confirmCode);
		destinationService = serverClass.getServiceById(destServiceId);
	}

	@Override
	public void run() {
		try {
			login();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// login succeeded now connect to another client that is also logged in
		// the server. Perform the client-client connection protocol with the
		// server service- TODO call initProtocol() here
		while (inService) {
			if (protocol != null && !protocol.work())
				inService = false;
		}
		stopService();
	}

	public SSCServer getServerClass() {
		return serverClass;
	}

	@Override
	public void forwardMessageToService(SSCServerService service, byte[] em,
			byte[] hm) {

	}

	/**
	 * 
	 * @return the generated Id for the new client.
	 */
	private int generateId() {
		boolean retry = true;
		int id = -1;
		while (retry) {
			retry = false;
			id = 555555 + rand.nextInt(444444);
			if (serverClass.getClientServices().size() == 0)
				return id;
			for (SSCServerService service : serverClass.getClientServices()) {
				if (service.getServiceId() == id)
					retry = true;
			}
		}
		return id;
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
	public int getServiceId() {
		return serviceId;
	}

	@Override
	public SSCServerService getDestService() {
		return destinationService;
	}

	@Override
	public void addMessageToBuffer(byte[] m) {
		serverClass.getBuffer().add(m, serverBufferId);
	}

}
