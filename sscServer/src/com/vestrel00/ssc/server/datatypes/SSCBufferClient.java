package com.vestrel00.ssc.server.datatypes;

/**
 * Used for storing buffer related data pertaining to one client.
 * 
 * @author Estrellado, Vandolf
 * 
 */
public class SSCBufferClient {

	private int bufferId, bufferSize;

	public SSCBufferClient(int bufferId, int bufferSize) {
		this.bufferId = bufferId;
		this.bufferSize = bufferSize;
	}

	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}
	
	public int getBufferId(){
		return bufferId;
	}
	
	public int getBufferSize(){
		return bufferSize;
	}

}
