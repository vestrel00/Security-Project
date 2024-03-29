package com.vestrel00.ssc.client;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds all incoming and outgoing messages as plain text. This is quite
 * different from the server buffer, which is more complex.
 * 
 * @author Estrellado, Vandolf
 * 
 */
public class SSCClientBuffer {

	private List<String> buffer;
	private int maxSize;

	public SSCClientBuffer(int maxSize) {
		this.maxSize = maxSize;
		buffer = new ArrayList<String>();
	}

	public void add(String message) {
		if (buffer.size() >= maxSize)
			buffer.remove(0);
		buffer.add(message);
	}

	public void clear() {
		buffer.clear();
	}

	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}

}
