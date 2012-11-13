package com.vestrel00.ssc.server.interf;

import com.vestrel00.ssc.server.interf.SSCCrypto;

/**
 * Protocol that the client and server programs run.
 * 
 * @author Estrellado, Vandolf
 * 
 */
public interface SSCProtocol {

	/**
	 * Initialize the crypto that will be used.
	 */
	public void initCrypto(String secretKey, String keyCodeOK);

	/**
	 * Tells the protocol to keep handling requests by the client. If the client
	 * quits, this returns false, flagging the service to stop.
	 */
	public boolean work();

	/**
	 * This is where the magic of the protocol happens. To be called inside the
	 * work() method.
	 */
	public void performMagic();

	/**
	 * Calling work() after this method is called will have no effect();
	 */
	public void stopWorking();
	
	/**
	 * Return the crypto in use.
	 */
	public SSCCrypto getCrypto();

}
