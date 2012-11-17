package com.vestrel00.ssc.server.datatypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Used for storing buffer related data pertaining to one client.
 * 
 * @author Estrellado, Vandolf
 * 
 */
public class SSCBufferClient {

	private static final CharSequence NL = "\n";

	private List<byte[]> buffer;
	private int bufferId, bufferSize, maxBufferSize;
	private StringBuilder builder;

	public SSCBufferClient(int bufferId, int bufferSize, int maxBufferSize) {
		this.bufferId = bufferId;
		this.maxBufferSize = maxBufferSize;
		setBufferSize(bufferSize);
		buffer = new ArrayList<byte[]>();
		builder = new StringBuilder();
	}

	/**
	 * Sets the size of the buffer. The given size may not exceed the maximum
	 * size set by the server at the time of allocation. If it does, it will be
	 * set to the maximum set by the server.
	 */
	public void setBufferSize(int bufferSize) {
		if (bufferSize < maxBufferSize)
			this.bufferSize = bufferSize;
		else
			this.bufferSize = maxBufferSize;
	}

	/**
	 * Returns all the messages contained in the given bufferId as strings. Each
	 * message is separated by a new line from oldest to current.
	 */
	@Override
	public String toString() {
		builder.delete(0, builder.length());
		for (byte[] b : buffer) {
			builder.append(new String(b));
			builder.append(NL);
		}
		return builder.toString();
	}

	public int getBufferId() {
		return bufferId;
	}

	public int getBufferSize() {
		return bufferSize;
	}

	public int getCurrentSize() {
		return buffer.size();
	}

	public void addMessage(byte[] m) {
		if (buffer.size() >= bufferSize)
			buffer.remove(0);
		buffer.add(m);
	}

	public void clear() {
		buffer.clear();
	}

}
