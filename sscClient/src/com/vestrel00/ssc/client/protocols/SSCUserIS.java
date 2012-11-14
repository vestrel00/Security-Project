package com.vestrel00.ssc.client.protocols;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.vestrel00.ssc.client.SSCClient;
import com.vestrel00.ssc.client.shared.SSCStreamManager;

/**
 * Runnable that handles user input. This is necessary since client program must
 * listen for user input and server input.
 * 
 * @author Estrellado, Vandolf
 * 
 */
public class SSCUserIS implements Runnable {

	private SSCClient client;
	private String userStr;

	public SSCUserIS(SSCClient client) {
		this.client = client;
	}

	/**
	 * Starts listening for USER input. User input is streamed out to the server
	 * service. <br>
	 * <b>Procedure: </b>
	 * <ol>
	 * <li>E(m) is sent to server</li>
	 * <li>Client waits for server OK</li>
	 * <li>Client sends H(m)</li>
	 * </ol>
	 * Server checks if H(D(E(m))) == H(m). If so, m is stored in its buffer as
	 * a byte[] and E(m) and H(m) is forwarded to the receiving client.
	 * 
	 * @throws IOException
	 */
	@Override
	public void run() {
		try {
			while ((userStr = client.getUserInputStream().readLine()) != null) {
				client.getBuffer().add(userStr);
				// send E(m)
				SSCStreamManager.sendBytes(
						client.getOutputStream(),
						client.getClientProtocol().getCrypto()
								.encrypt(userStr.getBytes()));

				// wait for server E(OK)
				byte[] resultCode = client
						.getClientProtocol()
						.getCrypto()
						.decrypt(
								SSCStreamManager.readBytes(client
										.getServerInputStream()));

				boolean confirmed = true;
				for (int i = 0; i < resultCode.length; i++)
					if (resultCode[i] != client.getClientProtocol().getCrypto()
							.getConfirmCode()[i]) {
						confirmed = false;
						break;
					}

				if (confirmed)
					// send H(m)
					SSCStreamManager.sendBytes(
							client.getOutputStream(),
							MessageDigest.getInstance("SHA-1").digest(
									userStr.getBytes()));
				else
					continue;// something went wrong - do nothing
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
}
