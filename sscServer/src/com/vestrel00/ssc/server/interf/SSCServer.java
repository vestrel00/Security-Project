package com.vestrel00.ssc.server.interf;

import java.io.IOException;

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
	 * connection requests. Otherwise, the server is shutdown;
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
	public void stopListening() throws IOException;

}