package com.vestrel00.ssc.server;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import com.vestrel00.ssc.server.datatypes.SSCPendingClient;
import com.vestrel00.ssc.server.datatypes.SSCServerClient;
import com.vestrel00.ssc.server.interf.SSCCryptoPublic;
import com.vestrel00.ssc.server.interf.SSCServer;
import com.vestrel00.ssc.server.interf.SSCServerService;
import com.vestrel00.ssc.server.shared.SSCCryptoRSA;
import com.vestrel00.ssc.server.shared.SSCFileManager;
import com.vestrel00.ssc.server.shared.SSCSettings;
import com.vestrel00.ssc.server.shared.SSCStreamManager;

/**
 * An implementation of an SSCServer.
 * 
 * @author Estrellado, Vandolf
 * @see SSCServer
 * 
 */
public class SSCServerStandard implements SSCServer {

	private SSCSettings settings;
	private ServerSocket server;
	private List<SSCPendingClient> pending;
	private List<SSCServerService> clientServices;
	private SSCServerBuffer buffer;
	private SSCCryptoPublic pubCrypt;
	private PublicKey pubKey;
	private boolean isListening;
	private int maxClientCount;

	public SSCServerStandard(int port, int maxClientCount,
			int maxClientBufferSize) throws IOException {
		server = new ServerSocket(port);
		pending = new ArrayList<SSCPendingClient>();
		clientServices = new ArrayList<SSCServerService>();
		buffer = new SSCServerBuffer(maxClientCount, maxClientBufferSize);
		settings = new SSCSettings();
		// initialize Public Crypto
		// Note that server will not be using the public key
		// since it will not encrypt anything - but store it anyways
		pubKey = SSCFileManager.readPublicFromFile("keys/public.key");
		pubCrypt = new SSCCryptoRSA(
				SSCFileManager.readPrivateFromFile("keys/private.key"), pubKey);
		isListening = true;
	}

	public void start() {
		new Thread(new AdminCommand()).start();
		while (isListening) {
			try {
				if (clientServices.size() >= maxClientCount) {
					new Thread(new ConnectionReception(this, server.accept()))
							.start();
				}
			} catch (IOException e) {
				finish();
			}
		}
		finish();
	}

	public void finish() {
		if (isListening) {
			isListening = false;
			try {
				server.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				int size = clientServices.size();
				for (int i = 0; i < size; i++)
					if (clientServices.get(0).getClient().getName() != null)
						clientServices.get(0).stopService(true);

				// double check
				size = clientServices.size();
				for (int i = 0; i < size; i++)
					clientServices.get(0).stopService(true);
			} catch (IndexOutOfBoundsException e) {
				// this might arise due to multi threading
			}

			SSCServerDB.finish();
			System.out.println("Server successfully shutdown.");
		}
	}

	public SSCServerBuffer getBuffer() {
		return buffer;
	}

	@Override
	public SSCServerService getServiceByName(String clientName) {
		for (SSCServerService service : clientServices) {
			// TODO REMOVE once nulls are replaced with numbers
			if (service.getClient().getName() != null)
				if (service.getClient().getName().contentEquals(clientName))
					return service;
		}
		return null;
	}

	@Override
	public List<SSCServerService> getClientServices() {
		return clientServices;
	}

	@Override
	public boolean clientIsOnline(String clientName) {
		for (SSCServerService serv : clientServices)
			if (serv.getClient().getName() != null
					&& serv.getClient().getName().contentEquals(clientName))
				return true;
		return false;
	}

	// TODO Once a client connects, it is added onto the service
	// list, however with name = null. So if more than one client is
	// connected but has not logged in, how can we tell which client
	// to remove if their name is null? Answer is to make name as an
	// integer if client is connected but not logged in instead of
	// null.
	@Override
	public void removeService(String clientName, int clientBufferId) {
		int index = -1;
		if (clientName == null)
			return;

		for (int i = 0; i < clientServices.size(); i++)
			if (clientServices.get(i).getClient().getName() != null)
				if (clientServices.get(i).getClient().getName()
						.contentEquals(clientName)) {
					index = i;
					break;
				}
		if (index != -1)
			clientServices.remove(index);
		buffer.removeClientById(clientBufferId);
		System.out.println("User " + clientName + " has logged out.");
	}

	@Override
	public List<SSCPendingClient> getPendingClients() {
		return pending;
	}

	@Override
	public SSCPendingClient retrievePendingClient(String name) {
		boolean retry = true;
		SSCPendingClient pc = null;
		int index = 0;
		// can insert a wait here from the client before starting the loop to
		// minimize cpu but ehhhhhhhh optimization my ass - I just wanna finish
		// this lol
		while (retry) {
			// TODO WHY DO WE NEED TO FLUSH SYSTEM.OUT????
			// same thing for
			System.out.flush();
			for (index = 0; index < pending.size(); index++) {
				if (pending.get(index).getName().contentEquals(name)) {
					pc = pending.get(index);
					retry = false;
					break;
				}
			}
		}
		pending.remove(index);
		return pc;
	}

	@Override
	public SSCSettings getSettings() {
		return settings;
	}

	/**
	 * Decides what to do with the accepted socket connection. For each accepted
	 * client, checks if the client is already connected. If so, it places the
	 * client in the pending client list which is checked by initReceiver()
	 * during the connection() protocol.
	 * 
	 * @author Estrellado, Vandolf
	 * @see SSCServerService#initReceiver()
	 * 
	 */
	private class ConnectionReception implements Runnable {

		private Socket client;
		private SSCServer serverClass;

		private ConnectionReception(SSCServer serverClass, Socket client) {
			this.serverClass = serverClass;
			this.client = client;
		}

		/**
		 * Asks for the client's user name. If the client has not logged in, it
		 * is in the init() protocol and the username it will send is null and a
		 * new service and client data type will be created. If the client is
		 * logged in, then it is in the initSender() protocol and its service is
		 * in the initReceiver() protocol and this new socket is placed in the
		 * pending list as an SSCPendingClient. <br>
		 * Also sends over the public key - to AMEND!.
		 * 
		 * @see com.vestrel00.ssc.server.datatypes.SSCPendingClient
		 */
		@Override
		public void run() {
			// temporary streams
			// the references to these streams are not saved or passed
			// change it if not lazy
			DataInputStream in = null;
			DataOutputStream out = null;
			byte[] uname = null;

			try {
				in = new DataInputStream(client.getInputStream());
				out = new DataOutputStream(client.getOutputStream());

				// send PublicKey - DANGEROUS!
				// public key should be distributed with client program
				// not sent like this on connect! TODO?
				ObjectOutputStream oout = new ObjectOutputStream(out);
				oout.writeObject(pubKey);
				// do not close the stream! it will still be used
				// this is actually done twice- b4 login and on connect()

				// wait for E(username)
				uname = pubCrypt.decrypt(SSCStreamManager.readBytes(in));
				SSCStreamManager.sendBytes(out, "OK".getBytes());
			} catch (IOException e) {
				// e.printStackTrace();
			}

			if (new String(uname).contentEquals("null")) {
				try {
					int bufferSize;
					bufferSize = Integer.parseInt(new String(SSCStreamManager
							.readBytes(in)));
					SSCSServiceStandard service = new SSCSServiceStandard(
							serverClass, new SSCServerClient(
									buffer.allocate(bufferSize), client, null),
							pubCrypt);
					clientServices.add(service);
					new Thread(service).start();
				} catch (NumberFormatException | IOException e) {
					e.printStackTrace();
				}
			} else
				pending.add(new SSCPendingClient(new String(uname), client));

			// NOTE: Do not close the socket's in/out streams
			// the service will do that itself - not the server.
		}
	}

	/**
	 * Admin input handler.
	 * 
	 * @author Estrellado, Vandolf
	 * 
	 */
	private class AdminCommand implements Runnable {

		@Override
		public void run() {
			BufferedReader in = new BufferedReader(new InputStreamReader(
					System.in));
			StringBuilder builder = new StringBuilder();
			int commandIndex, itemIndex, flagIndex, paramIndex;
			String command, item, flag, param = null;

			System.out.println("Server started");
			help();
			while (isListening) {
				builder.delete(0, builder.length());
				try {
					builder.append(in.readLine());
				} catch (IOException e) {
					e.printStackTrace();
				}
				commandIndex = builder.indexOf(" ");
				if (commandIndex == -1) { // command
					command = builder.toString();
					if (command.contentEquals("help"))
						help();
					else if (command.contentEquals("list")) {
						if (clientServices.size() == 0)
							System.out
									.println("There are currently no users online.");
						else
							printLoggedInUsers();
					} else if (command.contentEquals("shutdown"))
						finish();
				} else { // command flag
					command = builder.substring(0, commandIndex);
					flagIndex = builder.indexOf("-") + 1;
					flag = builder.substring(flagIndex, flagIndex + 1);
					if (flagIndex == commandIndex + 2) {
						if (command.contentEquals("print")) {
							paramIndex = builder.lastIndexOf(" ") + 1;
							param = builder.substring(paramIndex);
							if (flag.contentEquals("m")) {
								printClientBuffer(param);
							}
						}
					} else if (flagIndex == 0) {// command param
						paramIndex = builder.lastIndexOf(" ") + 1;
						param = builder.substring(paramIndex);
						if (command.contentEquals("kick")) {
							try {
								getServiceByName(param).stopService(true);
							} catch (NullPointerException e) {
								System.out.println("User " + param
										+ " is not online.");
							}
						} else if (command.contentEquals("get")) {
							if (param.contentEquals("port"))
								System.out.println(server.getLocalPort());
						}
					} else { // command item flag <param>
						itemIndex = commandIndex + 1;
						item = builder.substring(itemIndex, flagIndex - 2);
						paramIndex = builder.lastIndexOf(" ") + 1;
						if (paramIndex != -1)
							param = builder.substring(paramIndex);
						if (command.contentEquals("clear")) {
							if (item.contentEquals("buffer")) {
								if (clientServices.size() == 0)
									System.out
											.println("There are no buffers to clear.");
								else if (flag.contentEquals("a")) {
									for (SSCServerService serv : clientServices)
										serv.getClient().getBuffer().clear();
									System.out
											.println("All buffers have been cleared.");
								} else if (flag.contentEquals("n")) {
									try {
										getServiceByName(param).getClient()
												.getBuffer().clear();
										System.out
												.println("User "
														+ param
														+ "'s buffer has been cleared.");
									} catch (NullPointerException e) {
										System.out.println("User " + param
												+ "is not online.");
									}
								}
							}
						} else if (command.contentEquals("modify")) {
							if (item.contentEquals("settings")) {
								if (flag.contentEquals("d")) {
									if (param.contentEquals("receiver"))
										settings.debugReceiverProtocol = true;
									else if (param.contentEquals("sender"))
										settings.debugSenderProtocol = true;
								} else if (flag.contentEquals("h")) {
									if (param.contentEquals("receiver"))
										settings.debugReceiverProtocol = false;
									else if (param.contentEquals("sender"))
										settings.debugSenderProtocol = false;
								}

							}
						}
					}

				}

			}

			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Prints the messages in the client's buffer.
		 */
		private void printClientBuffer(String name) {
			try {
				SSCServerService serv = getServiceByName(name);
				if (serv.getClient().getBuffer().getCurrentSize() != 0)
					System.out.print(serv.getClient().getBuffer().toString());
				else
					System.out.println("User " + name
							+ " has not sent any messages");
			} catch (NullPointerException e) {
				System.out.println("User " + name + " is not online.");
			}

		}

		/**
		 * We can't just check the size of clientServices. Some users are
		 * connected but have not yet logged in. These users at that point have
		 * name 'null'".
		 */
		// TODO Once a client connects, it is added onto the service
		// list, however with name = null. So if more than one client is
		// connected but has not logged in, how can we tell which client
		// to remove if their name is null? Answer is to make name as an
		// integer if client is connected but not logged in instead of
		// null.
		private void printLoggedInUsers() {
			String users = "";
			int count = 0;
			for (SSCServerService serv : clientServices) {
				if (serv.getClient().getName() != null) {
					count++;
					users += serv.getClient().getName() + ", ";
				}
			}
			System.out.println("There are currently " + count
					+ " user(s) online.");
			System.out.println(users);
		}

		private void help() {
			System.out.println("Enter command <item> <flag> <parameter>:");
			System.out
					.println("clear buffer -a : clears the buffer of all services");
			System.out
					.println("clear buffer -n <clientName> : clears the client buffer <clientName>");
			System.out
					.println("get port : prints out the port number the server is listening on.");
			System.out.println("help : prints out this list");
			System.out
					.println("kick <clientName> : kicks the <clientName> from the server.");
			System.out.println("list : lists all the active services");
			System.out
					.println("modify settings -d sender : enables sender protocol dedbug mode");
			System.out
					.println("modify settings -d receiver : enables receiver protocol dedbug mode");
			System.out
					.println("modify settings -h sender : disables sender protocol dedbug mode");
			System.out
					.println("modify settings -h receiver : disables receiver protocol dedbug mode");
			System.out
					.println("print -m <clientName> : prints the messages in the buffer <clientName>");
			System.out.println("shutdown : shuts down the server");
		}

	}

}
