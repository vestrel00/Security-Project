package com.vestrel00.ssc.server.protocols;

import java.io.IOException;
import java.net.UnknownHostException;

import com.vestrel00.ssc.server.shared.SSCCryptoAES;
import com.vestrel00.ssc.server.interf.SSCCrypto;
import com.vestrel00.ssc.server.interf.SSCProtocol;
import com.vestrel00.ssc.server.interf.SSCServerService;

/**
 * This is the protocol that the server runs for each client.
 * 
 * @author Estrellado, Vandolf
 * 
 */
public class SSCServerProtocol implements SSCProtocol {

	private SSCServerService service;
	private SSCCrypto crypt;
	private boolean isWorking;

	/**
	 * Initialize the protocol but without initializing the crypto. Must
	 * manually call initCrypto if this constructor is used!
	 */
	public SSCServerProtocol(SSCServerService service) {
		this.service = service;
		isWorking = true;
	}

	/**
	 * Initialize the protocol including the crypto.
	 */
	public SSCServerProtocol(SSCServerService service, String secretKey,
			String keyCodeOK) {
		this(service);
		initCrypto(secretKey, keyCodeOK);
	}

	@Override
	public void initCrypto(String secretKey, String keyCodeOK) {
		try {
			crypt = new SSCCryptoAES(secretKey.getBytes(), keyCodeOK);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean work() {
		if (isWorking) {
			performMagic();
			return true;
		}
		return false;
	}

	@Override
	public void performMagic() {

	}

	@Override
	public void stopWorking() {
		isWorking = false;
	}

	@Override
	public SSCCrypto getCrypto() {
		return crypt;
	}

}
