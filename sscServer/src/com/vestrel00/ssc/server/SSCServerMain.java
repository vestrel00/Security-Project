package com.vestrel00.ssc.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.sql.SQLException;

import com.vestrel00.ssc.server.interf.SSCServer;
import com.vestrel00.ssc.server.shared.SSCFileManager;

/**
 * Starts the server program of SSC.
 * 
 * @author Estrellado, Vandolf
 * 
 */
public class SSCServerMain {

	private static int port, maxClientCount, maxClientBufferSize;
	private static BufferedReader in;
	private static SSCServer server;

	public static void main(String args[]) throws IOException {
		in = new BufferedReader(new InputStreamReader(System.in));
		// calling in.close closes the System.in stream. Don't do it.
		System.out.println("Hello non-shady person!");
		System.out.println("This is Vandolf Estrellado's 480 project.");
		enterPort();
		enterCapacity();
		enterBufferSize();
		initDB();
		initPublicCrypto();
		initServer();
	}

	/**
	 * Generate and save an RSA key pair if it does not yet exist. Otherwise, it
	 * does nothing.
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	private static void initPublicCrypto() throws FileNotFoundException, IOException {
		// check if key files exist
		File dir = new File("keys");
		if (dir.exists())
			return;
		
		// create empty files for the keys
		new File("keys").mkdir();
		new File("keys/public.key").createNewFile();
		new File("keys/private.key").createNewFile();
		
		System.out.println("Creating RSA Keypair...");
		try {
			// generate keys
			KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
			// gen.initialize(2048);
			 // for faster decryption
			// not as secure as 2048 and don't last as long
			// but does the job anyways for a small project
			gen.initialize(1024);
			KeyPair pair = gen.generateKeyPair();
			// Key pubKey = pair.getPublic();
			// Key privKey = pair.getPrivate();
			// save keys
			KeyFactory fact = KeyFactory.getInstance("RSA");
			RSAPublicKeySpec pubSpec = fact.getKeySpec(pair.getPublic(),
					RSAPublicKeySpec.class);
			RSAPrivateKeySpec privSpec = fact.getKeySpec(pair.getPrivate(),
					RSAPrivateKeySpec.class);
			
			SSCFileManager.saveToFile("keys/public.key", pubSpec.getModulus(),
					pubSpec.getPublicExponent());
			SSCFileManager.saveToFile("keys/private.key", privSpec.getModulus(),
					privSpec.getPrivateExponent());
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			e.printStackTrace();
		}
	}

	private static void initDB() {
		System.out.println("Initializing database...");
		System.out
				.println("This may take 20 seconds if the database has not been created.");
		try {
			SSCServerDB.init();
		} catch (InstantiationException | IllegalAccessException
				| ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
	}

	private static void initServer() throws IOException {
		try {
			server = new SSCServerStandard(port, maxClientCount,
					maxClientBufferSize);
			server.start();
		} catch (IOException e) {
			System.out.println("Port " + port
					+ " is already in use.\nEnter another port");
			enterPort();
			initServer();
		}
	}

	private static void enterPort() throws IOException {
		try {
			System.out
					.println("Enter the port number you wish to use for the server");
			port = Integer.parseInt(in.readLine());
		} catch (NumberFormatException e) {
			System.out.println("Invalid number format.");
			enterPort();
		}
	}

	private static void enterCapacity() throws IOException {
		try {
			System.out.println("Enter the maximum number of clients "
					+ "you want your server to be\nable handle at once");
			maxClientCount = Integer.parseInt(in.readLine());
		} catch (NumberFormatException e) {
			System.out.println("Invalid number format.");
			enterCapacity();
		}
	}

	private static void enterBufferSize() throws IOException {
		try {
			System.out.println("Enter the maximum buffer size for each client");
			maxClientBufferSize = Integer.parseInt(in.readLine());
		} catch (NumberFormatException e) {
			System.out.println("Invalid number format.");
			enterBufferSize();
		}
	}

}
