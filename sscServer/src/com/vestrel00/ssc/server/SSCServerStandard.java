package com.vestrel00.ssc.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.vestrel00.ssc.server.interf.SSCServer;
import com.vestrel00.ssc.server.interf.SSCServerService;

/**
 * An implementation of an SSCServer.
 * 
 * @author Estrellado, Vandolf
 * @see SSCServer
 * 
 */
public class SSCServerStandard implements SSCServer {

	private ServerSocket server;
	private List<SSCServerService> clientServices;
	private SSCServerBuffer buffer;
	private boolean isListening;
	private int maxClientCount;

	public SSCServerStandard(int port, int maxClientCount,
			int maxClientBufferSize) throws IOException {
		server = new ServerSocket(port);
		clientServices = new ArrayList<SSCServerService>();
		buffer = new SSCServerBuffer(maxClientCount, maxClientBufferSize);
		isListening = true;
	}

	public void start() {
		new Thread(new AdminCommand()).start();
		while (isListening) {
			try {
				if (clientServices.size() >= maxClientCount) {
					Socket newClient = server.accept();
					clientServices
							.add(new SSCSServiceStandard(this, newClient));
					new Thread(clientServices.get(clientServices.size() - 1))
							.start();
				}
			} catch (IOException e) {
				System.out.println("Server successfully shutdown.");
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
			for (int i = 0; i < clientServices.size(); i++) {
				clientServices.get(i).stopService();
			}
		}
	}

	public SSCServerBuffer getBuffer() {
		return buffer;
	}

	@Override
	public SSCServerService getServiceByName(String clientName) {
		for (SSCServerService service : clientServices) {
			if (service.getClientName().contentEquals(clientName))
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
			if (serv.getClientName() != null
					&& serv.getClientName().contentEquals(clientName))
				return true;
		return false;
	}

	@Override
	public void removeService(String clientName, int clientBufferId) {
		int index = -1;
		for (int i = 0; i < clientServices.size(); i++)
			if (clientServices.get(i).getClientName().contentEquals(clientName)) {
				index = i;
				break;
			}
		if (index != -1)
			clientServices.remove(index);
		buffer.removeClientById(clientBufferId);
		System.out.println("User " + clientName + " has logged out.");
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
			String command, item, flag, param;

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
								getServiceByName(param).stopService();
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
						if (command.contentEquals("clear")) {
							if (item.contentEquals("buffer")) {
								if (clientServices.size() == 0)
									System.out
											.println("There are no buffers to clear.");
								else if (flag.contentEquals("a")) {
									for (SSCServerService serv : clientServices)
										serv.getClientBuffer().clear();
									System.out
											.println("All buffers have been cleared.");
								} else if (flag.contentEquals("n")) {
									paramIndex = builder.lastIndexOf(" ") + 1;
									param = builder.substring(paramIndex);
									try {
										getServiceByName(param)
												.getClientBuffer().clear();
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
				if (serv.getClientBuffer().getCurrentSize() != 0)
					serv.getClientBuffer().toString();
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
		private void printLoggedInUsers() {
			String users = "";
			int count = 0;
			for (SSCServerService serv : clientServices) {
				if (serv.getClientName() != null) {
					count++;
					users += serv.getClientName() + ", ";
				}
			}
			System.out.println("There are currently " + count
					+ " user(s) online.");
			System.out.println(users);
		}

		private void help() {
			System.out.println("The following commands are available:");
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
					.println("print -m <clientName> : prints the messages in the buffer <clientName>");
			System.out.println("shutdown : shuts down the server");
		}

	}

}
