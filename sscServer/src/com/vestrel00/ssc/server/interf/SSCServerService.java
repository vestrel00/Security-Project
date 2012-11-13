package com.vestrel00.ssc.server.interf;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * The server's response to a client connection request. This service is
 * dedicated to performing the protocol for the client and synchronizing with
 * the server that launched its service.
 * 
 * @author Estrellado, Vandolf
 * 
 */
public interface SSCServerService extends Runnable {

	/**
	 * Close all the streaming connections including the client socket.
	 * 
	 * @throws IOException
	 */
	public void stopService();

	/**
	 * Perform the login protocol with the client and then constantly check the
	 * state of the service. If the service is still working, the protocol is
	 * performed.
	 */
	@Override
	public void run();

	/**
	 * 
	 * @return the server class in use by this service
	 */
	public SSCServer getServerClass();

	/**
	 * 
	 * @return the input stream
	 */
	public DataInputStream getInputStream();

	/**
	 * 
	 * @return the output stream.
	 */
	public DataOutputStream getOutputStream();

	/**
	 * 
	 * @return the assigned id.
	 */
	public int getServiceId();

	/**
	 * Forwards E(m) and H(m) to the given service's client.
	 */
	public void forwardMessageToService(SSCServerService service, byte[] em,
			byte[] hm);

	/**
	 * Initialize the protocol with the given destination serviceId and agreed
	 * upon secretKey and confirmCode. All of the parameters are instanced (not
	 * saved in server database)
	 */
	public void initProtocol(int serviceId, String secretKey, String confirmCode);

	/**
	 * 
	 * @return the destination service (the service that the messages will be
	 *         sent to).
	 */
	public SSCServerService getDestService();

	/**
	 * Adds the given message to the buffer in the server.
	 */
	public void addMessageToBuffer(byte[] m);

}
