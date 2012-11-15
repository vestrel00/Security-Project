package com.vestrel00.ssc.client.interf;

/**
 * Protocol that the client and server programs run.
 * 
 * @author Estrellado, Vandolf
 * 
 */
public interface SSCProtocol {

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

}
