package com.vestrel00.ssc.server;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import com.vestrel00.ssc.server.datatypes.SSCBufferClient;

/**
 * This is used to hold all messages received from all clients. The buffer
 * should not be plain text (String). If adversary Malice where to get access to
 * the server's buffer, we don't want Malice to immediately see the contents
 * without doing any work.
 * 
 * @author Estrellado, Vandolf
 * 
 */
public class SSCServerBuffer {

	private List<SSCBufferClient> clients;
	private final int maxClientCount, maxClientBufferSize;
	private SecureRandom rand;

	/**
	 * Create the server buffer.
	 * 
	 * @param maxClientCount
	 *            maximum amount of buffers that can be active at once.
	 * @param maxClientBufferSize
	 *            maximum buffer size of any client that requests for a buffer.
	 */
	public SSCServerBuffer(int maxClientCount, int maxClientBufferSize) {
		this.maxClientCount = maxClientCount;
		this.maxClientBufferSize = maxClientBufferSize;
		clients = new ArrayList<SSCBufferClient>();
		rand = new SecureRandom();
	}

	/**
	 * Allocates a new buffer in the list of buffers having the given size.
	 * 
	 * @return the buffer client object that has been added to the server buffer
	 *         list. Null if server has reached maximum capacity.
	 */
	public SSCBufferClient allocate(int bufferSize) {
		SSCBufferClient bc;
		if (clients.size() < maxClientCount) {
			bc = new SSCBufferClient(genId(), bufferSize, maxClientBufferSize);
			clients.add(bc);
			return bc;
		} else
			return null;
	}

	/**
	 * Generates a random id that is unused by any of the buffers in the list.
	 * This uses SecureRandom! Not java.util.Random. <b>Why bother with
	 * this?</b> <br>
	 * For obfuscation of the client and its buffer. So if an attacker somehow
	 * gets access to the list of buffers, it tells nothing of who the buffer
	 * belongs to unless he has the corresponding service that has a handle to
	 * the same buffer object with the same id.
	 */
	private int genId() {
		boolean retry = true;
		int id = -1;
		while (retry) {
			retry = false;
			id = 55555555 + rand.nextInt(44444444);
			for (SSCBufferClient client : clients)
				if (client.getBufferId() == id) {
					retry = true;
					continue;
				}
		}
		return id;
	}

	/**
	 * Remove the client's buffer with the given id from the list buffers in the
	 * server.
	 */
	public void removeClientById(int clientBufferId) {
		int index = 0;
		for (int i = 0; i < clients.size(); i++)
			if (clients.get(i).getBufferId() == clientBufferId) {
				index = i;
				break;
			}
		if (clients.size() != 0)
			clients.remove(index);
	}

}
