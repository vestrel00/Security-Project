package com.vestrel00.ssc.client.protocols;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import com.vestrel00.ssc.client.SSCClient;
import com.vestrel00.ssc.client.interf.SSCCryptoPrivate;
import com.vestrel00.ssc.client.shared.SSCByteMethods;
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
	private SSCCryptoPrivate crypt;
	private String userStr;
	private BufferedReader userIn;
	private DataOutputStream out;
	private DataInputStream in;
	private Socket socket;

	public SSCClientMessageSender(SSCClient client, Socket socket,
			SSCCryptoPrivate crypt) {
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
	 * <li>Client sends H(E(m))</li>
	 * </ol>
	 * Server checks if H(E(m)) == H(E(m)). If so, m is stored in its buffer as
	 * a byte[] and E(m) and H(E(m)) is forwarded to the receiving client.
	 * 
	 * @throws IOException
	 */
	@Override
	public void run() {
		String mStr;
		// wait for the other client to get to this point
		// will proceed to chat if OK
		// else client will finish() due to closed service socket
		// also ending this thread
		try {
			if (SSCByteMethods.equal(SSCStreamManager.readBytes(in),
					"OK".getBytes()))
				System.out.println("You are now chatting with "
						+ client.getPartnerName());
			else {
				System.out.println(client.getPartnerName()
						+ " is now chatting with someone else.\n"
						+ "You have been dumped.");
				closeIO();
				return;
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		byte[] resultCode;
		try {
			// wait for user input
			while ((userStr = userIn.readLine()) != null && client.isRunning()) {
				resultCode = null;
				mStr = client.getUserName() + " : " + userStr;
				client.getBuffer().add(mStr);
				// System.out.println(client.getUserName()
				// + "Sender: sending E(m)");
				// send E(m)
				byte[] em = crypt.encrypt(mStr.getBytes());
				SSCStreamManager.sendBytes(out, em);

				// wait for confirmCode
				SSCStreamManager.readBytes(client.getSender().getInputStream());

				// System.out.println(client.getUserName()
				// + "Sender: sending iv");
				try {
					// send iv to client partner
					SSCStreamManager.sendBytes(client.getSender()
							.getOutputStream(), crypt.getIv());

					// System.out.println(client.getUserName()
					// + "Sender: waiting for confirmCode");
					// wait for confirmCode
					resultCode = SSCStreamManager.readBytes(client.getSender()
							.getInputStream());
				} catch (IOException e) {
					try {
						client.finish();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}

				if (SSCByteMethods.equal(crypt.getConfirmCode(), resultCode)) {
					// System.out.println(client.getUserName()
					// + "Sender: sending H(m)");
					// send H(E(m))
					SSCStreamManager.sendBytes(out,
							MessageDigest.getInstance("SHA-1").digest(em));
				} else {
					// send dummy H(E(m))
					SSCStreamManager.sendBytes(
							out,
							MessageDigest.getInstance("SHA-1").digest(
									crypt.encrypt(String.valueOf(
											new SecureRandom().nextInt(9999))
											.getBytes())));
					System.out
							.println("Warning! Connection may be compromised.\n"
									+ "Message not sent.");
				}
			}
		} catch (IOException | NoSuchAlgorithmException e) {
		} finally {
			closeIO();
		}
	}

	public DataOutputStream getOutputStream() {
		return out;
	}

	public DataInputStream getInputStream() {
		return in;
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
