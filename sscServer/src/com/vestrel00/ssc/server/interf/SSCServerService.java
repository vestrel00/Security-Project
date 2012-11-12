package com.vestrel00.ssc.server.interf;

import java.io.IOException;

/**
 * The server's response to a client connection request. This service is
 * dedicated to performing the protocol for the client.
 * 
 * @author Estrellado, Vandolf
 * 
 */
public interface SSCServerService extends Runnable {

	/**
	 * Initialize the BufferedReader and PrintWriter that will be used for
	 * communicating with the client. Closes the in and out streams and creates
	 * new ones if streams already exist.
	 * 
	 * @throws IOException
	 */
	public void openIO() throws IOException;

	/**
	 * Closes the in and out streams.
	 * 
	 * @throws IOException
	 */
	public void closeIO() throws IOException;

	/**
	 * Close all the streaming connections including the client socket.
	 * 
	 * @throws IOException
	 */
	public void stopService() throws IOException;

	/**
	 * Check the state of the service. If the service is still working, the
	 * protocol is performed.
	 */
	@Override
	public void run();

}