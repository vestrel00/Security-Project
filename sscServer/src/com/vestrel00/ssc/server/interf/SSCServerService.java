package com.vestrel00.ssc.server.interf;

import com.vestrel00.ssc.server.datatypes.SSCServerClient;
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
	 * Close all the streaming connections including the client socket and
	 * buffer. Also removes the service from the server. This alerts and logs
	 * out the other client if they were in chat.
	 * 
	 * @param remove
	 *            true if you want this service to remove itself from the
	 *            server's list
	 * @throws IOException
	 */
	public void stopService(boolean remove);

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
	 * @return the partners service (the service that the messages will be sent
	 *         to).
	 */
	public SSCServerService getClientPartnerService();

	/**
	 * 
	 * @return true if this service's client is in the connect() protocol with
	 *         another client.
	 */
	public boolean isConnected();

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

	/**
	 * 
	 * @return return the client
	 */
	public SSCServerClient getClient();

	/**
	 * 
	 * @return true if client is in chat with another client.
	 */
	public boolean isInChat();

	/**
	 * Set this service to be in chat or not.
	 */
	public void setOnChat(boolean isInChat);

}
