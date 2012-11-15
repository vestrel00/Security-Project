package com.vestrel00.ssc.server.interf;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.vestrel00.ssc.server.datatypes.SSCBufferClient;
import com.vestrel00.ssc.server.protocols.SSCServerMessageReceiver;
import com.vestrel00.ssc.server.protocols.SSCServerMessageSender;

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
	 * Close all the streaming connections including the client socket. Also
	 * removes the service from the server.
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
	 * @return the other client's service (the service that the messages will be
	 *         sent to).
	 */
	public SSCServerService getOtherClientService();

	/**
	 * 
	 * @return the name of the client that is logged in.
	 */
	public String getClientName();

	/**
	 * 
	 * @return true if this service's client is already in chat with another
	 */
	public boolean isInChat();

	/**
	 * 
	 * @return the client's buffer object.
	 */
	public SSCBufferClient getClientBuffer();

	/**
	 * Used by the service that computed the keys.
	 */
	public void setSecretKey(byte[] secretKey);

	/**
	 * Used by the service that computed the keys.
	 */
	public void setConfirmCode(byte[] confirmCode);

	/**
	 * 
	 * @return the service's receiver.
	 */
	public SSCServerMessageReceiver getReceiver();

	/**
	 * 
	 * @return the service's sender.
	 */
	public SSCServerMessageSender getSender();

}
