package com.vestrel00.ssc.server;

import java.util.ArrayList;
import java.util.List;

import com.vestrel00.ssc.server.datatypes.SSCBufferClient;

/**
 * This is used to hold all messages received from all clients. The buffer
 * should not be plain text (String). If adversary Malice where to get a hold of
 * the buffer, we don't want Malice to immediately see the contents without
 * doing any work.
 * 
 * @author Estrellado, Vandolf
 * 
 */
public class SSCServerBuffer {

	private static final CharSequence NL = "\n";

	/**
	 * Double array. Each list in the buffer is allocated to each client.
	 */
	private List<ArrayList<byte[]>> buffers;
	private List<SSCBufferClient> clients;
	private int maxClientCount;

	private StringBuilder builder;

	/**
	 * Create the buffer.
	 * 
	 * @param bufferSize
	 *            The amount of memory this buffer can hold. Used for keeping
	 *            track of client and server history.
	 */
	public SSCServerBuffer(int maxClientCount) {
		this.maxClientCount = maxClientCount;
		clients = new ArrayList<SSCBufferClient>();
		buffers = new ArrayList<ArrayList<byte[]>>();
		builder = new StringBuilder();
	}

	/**
	 * Allocates a new buffer in the list of buffers having the given size.
	 * 
	 * @return the bufferId. -1 if the list of buffers has reached its capacity
	 *         and no new buffer is allocated
	 */
	public int allocate(int bufferSize) {
		if (buffers.size() < maxClientCount) {
			buffers.add(new ArrayList<byte[]>());
			clients.add(new SSCBufferClient(buffers.size() - 1, bufferSize));
			return buffers.size() - 1;
		} else
			return -1;
	}

	/**
	 * Adds a message to the buffer with the given buffer id. Note that adding a
	 * message when the buffer is full deletes the oldest message (queue/LIFO
	 * logic).
	 * 
	 * @param bufferId
	 *            the buffer to store the message in
	 * @param message
	 *            message to be saved in the buffer
	 */
	public void add(byte[] message, int bufferId) {
		if (buffers.get(bufferId).size() < clients.get(bufferId)
				.getBufferSize()) {
			buffers.get(bufferId).add(message);
		} else {
			buffers.get(bufferId).remove(0);
			buffers.get(bufferId).add(message);
		}
	}

	/**
	 * <b>Warning!</b><br>
	 * Clears all the buffers for all the clients.
	 */
	public void clearAll() {
		for (ArrayList<byte[]> l : buffers)
			l.clear();
	}

	public void clear(int bufferId) {

	}

	/**
	 * Sets the size of the buffer with the given id.
	 */
	public void setSize(int bufferSize, int bufferId) {
		clients.get(bufferId).setBufferSize(bufferSize);
	}

	/**
	 * Returns all the messages contained in the given bufferId as strings. Each
	 * message is separated by a new line from oldest to current.
	 */
	public String toStringOf(int bufferId) {
		builder.delete(0, builder.length());
		for (byte[] b : buffers.get(bufferId)) {
			builder.append(new String(b));
			builder.append(NL);
		}
		return builder.toString();
	}
}