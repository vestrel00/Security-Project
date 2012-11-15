package com.vestrel00.ssc.client.protocols;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.vestrel00.ssc.client.SSCClient;
import com.vestrel00.ssc.client.interf.SSCCrypto;
import com.vestrel00.ssc.client.shared.SSCStreamManager;

/**
 * Runnable that handles user input which is sent to the service. This is
 * necessary since client program must listen for user input and server input.
 * <b>This needs a new Socket to the server! Since it is not feasible or even
 * possible to use the same in and output streams concurrently in two threads.
 * Thus, by getting a new socket, different in and out streams may be
 * created!</b>
 * 
 * @author Estrellado, Vandolf
 * 
 */
public class SSCClientMessageSender implements Runnable {

	private SSCClient client;
	private SSCCrypto crypt;
	private String userStr;
	private BufferedReader userIn;
	private DataOutputStream out;
	private DataInputStream in;
	private Socket socket;

	public SSCClientMessageSender(SSCClient client, Socket socket,
			SSCCrypto crypt) {
		this.client = client;
		this.socket = socket;
		this.crypt = crypt;
		try {
			userIn = new BufferedReader(new InputStreamReader(System.in));
			out = new DataOutputStream(socket.getOutputStream());
			in = new DataInputStream(socket.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
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
			while ((userStr = userIn.readLine()) != null) {
				client.getBuffer().add(userStr);
				// send E(m)
				SSCStreamManager.sendBytes(out,
						crypt.encrypt(userStr.getBytes()));

				// wait for server E(OK)
				byte[] resultCode = crypt.decrypt(SSCStreamManager
						.readBytes(in));

				boolean confirmed = true;
				for (int i = 0; i < resultCode.length; i++)
					if (resultCode[i] != crypt.getConfirmCode()[i]) {
						confirmed = false;
						break;
					}

				if (confirmed)
					// send H(m)
					SSCStreamManager.sendBytes(
							out,
							MessageDigest.getInstance("SHA-1").digest(
									userStr.getBytes()));
				else
					continue;// something went wrong - do nothing
			}
		} catch (IOException | NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		// clean up
		try {
			userIn.close();
			out.close();
			in.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
