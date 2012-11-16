package com.vestrel00.ssc.server.interf;

import java.io.IOException;
import java.util.List;

import com.vestrel00.ssc.server.SSCServerBuffer;

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
public interface SSCServer {

	/**
	 * Starts the server. If the server is listening, it accepts all incoming
	 * connection requests launching a dedicated service for it. Otherwise, the
	 * server is shutdown. Only a maximum of maxClientCount may be connected at
	 * once. Connection requests while server is full is ignored.
	 * <br>
	 * TODO make a queue
	 * 
	 * @throws IOException
	 */
	public void start();

	/**
	 * Server stops listening for client connections requests and closes itself
	 * and all client services this launched. A new SSCServer needs to be
	 * created to start the service once more.
	 * 
	 * @throws IOException
	 */
	public void finish();

	/**
	 * 
	 * @return the buffer in use
	 */
	public SSCServerBuffer getBuffer();

	/**
	 * 
	 * @return the service with the given client name.
	 */
	public SSCServerService getServiceByName(String clientName);

	/**
	 * 
	 * @return the current list of services.
	 */
	public List<SSCServerService> getClientServices();

	/**
	 * 
	 * @return true if client is online.
	 */
	public boolean clientIsOnline(String clientName);

	/**
	 * Remove the service with the given client name from the list and
	 * deallocates its buffer from the server.
	 */
	public void removeService(String clientName, int clientBufferId);

}
