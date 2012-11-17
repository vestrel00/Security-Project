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
		String mStr;
		try {
			while ((userStr = userIn.readLine()) != null && client.isRunning()) {
				mStr = client.getUserName() + " : " + userStr;
				client.getBuffer().add(mStr);
				// System.out.println(client.getUserName()
				// + "Sender: sending E(m)");
				// send E(m)
				SSCStreamManager.sendBytes(out, crypt.encrypt(mStr.getBytes()));

				// System.out.println(client.getUserName()
				// + "Sender: waiting for E(confirmCode)");
				// wait for server E(OK)
				byte[] resultCode = crypt.decrypt(SSCStreamManager
						.readBytes(in));

				boolean confirmed = true;
				for (int i = 0; i < resultCode.length; i++)
					if (resultCode[i] != crypt.getConfirmCode()[i]) {
						confirmed = false;
						break;
					}

				if (confirmed) {
					// System.out.println(client.getUserName()
					// + "Sender: sending H(m)");
					// send H(m)
					SSCStreamManager.sendBytes(
							out,
							MessageDigest.getInstance("SHA-1").digest(
									mStr.getBytes()));
				} else {
					System.out
							.println("Warning! Connection may be compromised.");
				}
			}
		} catch (IOException | NoSuchAlgorithmException e) {
		} finally {
			closeIO();
		}
	}

	/**
	 * Close all i/o connections including the socket itself.
	 */
	public void closeIO() {
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
