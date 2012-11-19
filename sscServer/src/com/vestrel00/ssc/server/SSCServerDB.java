package com.vestrel00.ssc.server;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database used by the server to store user info using
 * org.apache.derby.jdbc.ClientDriver. This same server is used even of there
 * are multiple servers running! <br>
 * <b>User info:</b>
 * <ol>
 * <li>username</li>
 * <li>salt</li>
 * <li>Hashed password</li>
 * <li>Friends list</li>
 * <li>Sent pending invites</li>
 * <li>Received pending invites</li>
 * </ol>
 * 
 * @author Estrellado, Vandolf
 * @see
 * 
 */
public class SSCServerDB {

	private static final String dbURL = "jdbc:derby:SSCServerDB;create=true";
	private static Connection conn;
	private static final String CLIENTS = "Users", FRIENDS = "Friends";

	/**
	 * Initialize the database connection. Creates the database if it does not
	 * yet exist. The username and password is not specified thus uses the
	 * default schema name of APP.
	 * 
	 * @return
	 * 
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws SQLException
	 */
	public static void init() throws InstantiationException,
			IllegalAccessException, ClassNotFoundException, SQLException {
		// initialize the driver
		Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
		// init the connection
		conn = DriverManager.getConnection(dbURL);
		create();
	}

	/**
	 * Close the connections to the db.
	 */
	public static void finish() {
		try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create a new user. <b> Uses default system encoding which should be
	 * UTF-8. Meaning 1 byte = 1 char!</b>
	 * 
	 * @param name
	 *            of the client
	 * @param salt
	 *            byte[] of length 8
	 * @param saltedHashedPass
	 *            byte[] of length 20 that is H(salt + pass);
	 * @return true if user is successfully created. false if user already exist
	 *         or invalid characters or length.
	 * @throws SQLException
	 */
	public static boolean createUser(String name, byte[] salt,
			byte[] saltedHashedPass) {
		// check if user exist
		if (userExists(name))
			return false;

		// insert new user
		try {
			Blob saltBlob = conn.createBlob();
			saltBlob.setBytes(1L, salt);
			Blob hashedPassBlob = conn.createBlob();
			hashedPassBlob.setBytes(1L, saltedHashedPass);
			PreparedStatement ps = conn.prepareStatement("INSERT INTO "
					+ CLIENTS + " (name, salt, hashedPass) VALUES (? , "
					+ "?, ? )");
			ps.setString(1, name);
			ps.setBlob(2, saltBlob);
			ps.setBlob(3, hashedPassBlob);
			ps.execute();
			ps.close();
			saltBlob.free();
			hashedPassBlob.free();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * @return if user with name exists
	 * @throws SQLException
	 */
	public static boolean userExists(String name) {
		try {
			PreparedStatement st = conn.prepareStatement("SELECT name FROM "
					+ CLIENTS + " WHERE name = ?");
			st.setString(1, name);
			ResultSet result = st.executeQuery();
			if (result.next()) 
				return true;

			st.close();
			return false;
		} catch (SQLException e) {
			return false;
		}
	}

	/**
	 * 
	 * @return the salt value of the user's password
	 * @throws SQLException
	 */
	public static byte[] getSalt(String name) {
		try {
			PreparedStatement st = conn.prepareStatement("SELECT salt FROM "
					+ CLIENTS + " WHERE name = ?");
			st.setString(1, name);
			ResultSet result = st.executeQuery();
			result.next();
			Blob salt = result.getBlob(1);
			byte[] saltArr = salt.getBytes(1, (int) salt.length());
			st.close();
			return saltArr;
		} catch (SQLException e) {
			return null;
		}
	}

	/**
	 * 
	 * @return the client name's salted hashed password.
	 * @throws SQLException
	 */
	public static byte[] getSaltedHashedPass(String name) {
		try {
			PreparedStatement st = conn
					.prepareStatement("SELECT hashedPass FROM " + CLIENTS
							+ " WHERE name = ?");
			st.setString(1, name);
			ResultSet result = st.executeQuery();
			result.next();
			Blob hashedPass = result.getBlob(1);
			byte[] hpArr = hashedPass.getBytes(1, (int) hashedPass.length());
			st.close();
			return hpArr;
		} catch (SQLException e) {
			return null;
		}
	}

	/**
	 * Creates the the tables if they do not yet exist. Note that the db driver
	 * I'm using does not support "IF NOT EXISTS" clause.<br>
	 * Note that Clob data types contain a list of client IDs in a comma
	 * separated value format. Also note that the Friends table contains current
	 * friends, received/sent invites as Clobs.
	 * 
	 * 
	 * @throws SQLException
	 */
	private static void create() throws SQLException {
		Statement st = conn.createStatement();
		// CLIENTS
		try {
			st.executeUpdate("CREATE TABLE "
					+ CLIENTS
					+ " (id INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), "
					+ "name VARCHAR(50) NOT NULL, " + "salt BLOB(8), "
					+ "hashedPass BLOB(20), PRIMARY KEY(id, name))");
		} catch (SQLException e) {
			// DB already created
		}
		st.close();

		// FRIENDS
		st = conn.createStatement();
		try {
			st.executeUpdate("CREATE TABLE " + FRIENDS
					+ " (id INT PRIMARY KEY, "
					+ "friends CLOB, sentInvites CLOB, receivedInvites CLOB, "
					+ "FOREIGN KEY (id) REFERENCES " + CLIENTS + "(id))");
		} catch (SQLException e) {
			// DB already created
		}
		st.close();
	}

}