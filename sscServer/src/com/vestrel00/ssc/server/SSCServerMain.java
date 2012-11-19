package com.vestrel00.ssc.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;

import com.vestrel00.ssc.server.interf.SSCServer;

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
		initServer();
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
					+ "you want your server to be able handle at once");
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
