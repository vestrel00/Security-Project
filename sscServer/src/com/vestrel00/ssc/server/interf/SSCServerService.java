package com.vestrel00.ssc.server.interf;

import java.io.IOException;

/**
 * The server's response to a client connection request. This service is
 * dedicated to performing the protocol for the client and synchronizing with
 * the server that launched tis service.
 * 
 * @author Estrellado, Vandolf
 * 
 */
public interface SSCServerService extends Runnable {

	/**
	 * Initialize the in/out streams that will be used for communicating with
	 * the client. Closes the in and out streams and creates new ones if streams
	 * already exist.
	 * 
	 * @throws IOException
	 */
	public void openIO();

	/**
	 * Closes the in and out streams.
	 * 
	 * @throws IOException
	 */
	public void closeIO();

	/**
	 * Close all the streaming connections including the client socket.
	 * 
	 * @throws IOException
	 */
	public void stopService();

	/**
	 * Check the state of the service. If the service is still working, the
	 * protocol is performed.
	 */
	@Override
	public void run();

	/**
	 * 
	 * @return the server class in use by this service
	 */
	public SSCServer getServerClass();

	/**
	 * Forwards the message from this service to the given service.
	 */
	public void forwardMessageTo(SSCServerService service, byte[] message);

}
