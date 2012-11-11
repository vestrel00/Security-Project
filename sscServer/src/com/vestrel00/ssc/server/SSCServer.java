package com.vestrel00.ssc.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * The main server. This runnable forever listens for client socket connection
 * requests on the ServerSocket.
 * </p>
 * <p>
 * When a request comes in, this accepts the connection, creates a new
 * SSCServerService object to process it, hands it the socket returned from
 * accept, and starts the SSCServerService thread. Then the server goes back to
 * listening for connection requests
 * </p>
 * 
 * @author Estrellado, Vandolf
 * 
 */
public class SSCServer implements Runnable {

	private ServerSocket server;
	private List<SSCServerService> clientServices;
	private boolean isListening;

	/**
	 * Create the server with the given port number.
	 * 
	 * @throws IOException
	 */
	public SSCServer(int port) throws IOException {
		server = new ServerSocket(port);
		clientServices = new ArrayList<SSCServerService>();
		isListening = true;
	}

	/**
	 * Update the server and all its components. <b>This must be called
	 * repeatedly in order to accept more client connection requests!</b>
	 * 
	 * @throws IOException
	 */
	public void run() {
		while (server != null && isListening) {
			try {
				Socket newClient = server.accept();
				clientServices.add(new SSCServerService(newClient));
				new Thread(clientServices.get(clientServices.size() - 1)).start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			stopListening();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Server stops listening for client connections requests and closes itself
	 * and all client services this launched. A new SSCServer needs to be created to
	 * start the service once more.
	 * 
	 * @throws IOException
	 */
	public void stopListening() throws IOException {
		isListening = false;
		server.close();
		for(int i=0; i<clientServices.size();i++){
			clientServices.get(i).stopService();
		}
	}

}
