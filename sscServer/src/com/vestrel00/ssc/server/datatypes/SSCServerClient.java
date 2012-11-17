package com.vestrel00.ssc.server.datatypes;

import java.net.Socket;

/**
 * An extension of SSCPendingClient that holds a buffer and partner name.
 * 
 * @author Estrellado, Vandolf
 * @see SSCPendingClient
 * 
 */
public class SSCServerClient extends SSCPendingClient {

	private String partnerName;
	private SSCBufferClient buffer;

	public SSCServerClient(SSCBufferClient buffer, Socket socket, String name) {
		super(name, socket);
		this.buffer = buffer;
	}

	public void setPartnerName(String partnerName) {
		this.partnerName = partnerName;
	}

	/**
	 * 
	 * @return the name of the client this client is connected to
	 */
	public String getPartnerName() {
		return partnerName;
	}

	public SSCBufferClient getBuffer() {
		return buffer;
	}

}
