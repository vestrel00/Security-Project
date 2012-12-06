package com.vestrel00.ssc.server;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Database used by the server to store user info using
 * org.apache.derby.jdbc.ClientDriver. This same server is used even of there
 * are multiple servers running! Note that <b>order of the sql method calls
 * MATTER!</b><br>
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
	 * Create a new user in the CLIENTS table and an empty friend list in the
	 * FRIENDS table. <b> Uses default system encoding which should be UTF-8.
	 * Meaning 1 byte = 1 char!</b>
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

			// create insert self into FRIENDS table
			// get id first
			int id = getId(name);
			Clob friendsClob = conn.createClob();
			Clob enemiesClob = conn.createClob();
			Clob sentClob = conn.createClob();
			Clob receivedClob = conn.createClob();
			friendsClob.setString(1, "");
			enemiesClob.setString(1, "");
			sentClob.setString(1, "");
			receivedClob.setString(1, "");
			ps = conn.prepareStatement("INSERT INTO " + FRIENDS
					+ " VALUES (?, ?, ?, ?, ?)");
			ps.setInt(1, id);
			ps.setAsciiStream(2, friendsClob.getAsciiStream());
			ps.setAsciiStream(3, enemiesClob.getAsciiStream());
			ps.setAsciiStream(4, sentClob.getAsciiStream());
			ps.setAsciiStream(5, receivedClob.getAsciiStream());
			ps.execute();
			ps.close();
			friendsClob.free();
			enemiesClob.free();
			sentClob.free();
			receivedClob.free();

			// need these here to avoid exception
			saltBlob.free();
			hashedPassBlob.free();

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * 
	 * @return the id of the user with the given name. Returns -1 if user does
	 *         not exist.
	 */
	public static int getId(String name) {
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT id FROM "
					+ CLIENTS + " WHERE name = ?");
			ps.setString(1, name);
			ps.executeQuery();
			ResultSet result = ps.getResultSet();
			result.next();
			int id = result.getInt(1);
			ps.close();
			return id;
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;
		}
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
			return st.executeQuery().next();
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
			salt.free();
			return saltArr;
		} catch (SQLException e) {
			e.printStackTrace();
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
			hashedPass.free();
			return hpArr;
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Removes the friendToRemove from the user with name in the FRIENDS table.
	 */
	public static boolean removeFromFriends(StringBuilder builder, String name,
			String friendToRemove) {
		return removeFromList(builder, "friends", name, friendToRemove);
	}

	/**
	 * Removes the enemyToRemove from the user with name in the FRIENDS table.
	 */
	public static boolean removeFromEnemies(StringBuilder builder, String name,
			String enemyToRemove) {
		return removeFromList(builder, "enemies", name, enemyToRemove);
	}

	/**
	 * Removes the sentInviteToRemove from the user with name in the FRIENDS
	 * table.
	 */
	public static boolean removeFromSentInvites(StringBuilder builder,
			String name, String sentInviteToRemove) {
		return removeFromList(builder, "sentInvites", name, sentInviteToRemove);
	}

	/**
	 * Removes the receivedInviteToRemove from the user with name in the FRIENDS
	 * table.
	 */
	public static boolean removeFromReceivedInvites(StringBuilder builder,
			String name, String receivedInviteToRemove) {
		return removeFromList(builder, "receivedInvites", name, receivedInviteToRemove);
	}

	/**
	 * Removes the strToRemove from the user with the given name in the listType
	 * in the FRIENDS table. Needs to provide a StringBuilder to make sure that
	 * there are no threading issues.
	 */
	private static boolean removeFromList(StringBuilder builder, String listType,
			String name, String strToRemove) {
		builder.delete(0, builder.length());
		builder.append(getList(listType, name));
		int index = builder.indexOf(strToRemove);
		if (index != -1) {
			builder.delete(index, index + strToRemove.length() + 1);
			updateList(listType, builder.toString(), name);
			return true;
		}
		return false;
	}

	/**
	 * @return true if insertion is successful
	 */
	public static boolean insertSentInvite(String name, String newSentInvite) {
		return insertToList("sentInvites", name, newSentInvite);
	}

	/**
	 * @return true if insertion is successful
	 */
	public static boolean insertReceivedInvite(String name,
			String newReceivedInvite) {
		return insertToList("receivedInvites", name, newReceivedInvite);
	}

	/**
	 * 
	 * @return true if insertion is successful
	 */
	public static boolean insertEnemy(String name, String newEnemy) {
		return insertToList("enemies", name, newEnemy);
	}

	/**
	 * @return true if insertion is successful
	 */
	public static boolean insertFriend(String name, String newFriend) {
		return insertToList("friends", name, newFriend);
	}

	/**
	 * 
	 * @return the list of friends stored in the clob in the FRIENDS table as
	 *         <name>,<name>,...
	 */
	public static String getFriendList(String name) {
		return getList("friends", name);
	}

	/**
	 * 
	 * @return the list of received invites stored in the clob in the FRIENDS
	 *         table as <name>,<name>,...
	 */
	public static String getEnemyList(String name) {
		return getList("enemies", name);
	}

	/**
	 * 
	 * @return the list sent invites stored in the clob in the FRIENDS table as
	 *         <name>,<name>,...
	 */
	public static String getSentInvites(String name) {
		return getList("sentInvites", name);
	}

	/**
	 * 
	 * @return the list received invites stored in the clob in the FRIENDS table
	 *         as <name>,<name>,...
	 */
	public static String getReceivedInvites(String name) {
		return getList("receivedInvites", name);
	}

	/**
	 * Replaces the received invites list of the given user.
	 */
	public static void updateReceivedInvites(String name, String list) {
		updateList("receivedInvites", list, name);
	}

	/**
	 * Replaces the sent invites list of the given user
	 */
	public static void updateSentInvites(String name, String list) {
		updateList("sentInvites", list, name);
	}

	/**
	 * Replaces the list of listType in the FRIENDS table of the given user name
	 */
	private static void updateList(String listType, String list, String name) {
		try {
			int id = getId(name);
			Clob listClob = conn.createClob();
			listClob.setString(1, list);
			PreparedStatement ps = conn.prepareStatement("UPDATE " + FRIENDS
					+ " SET " + listType + " = ? WHERE id = ?");
			ps.setAsciiStream(1, listClob.getAsciiStream());
			ps.setInt(2, id);
			ps.execute();
			ps.close();
			listClob.free();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This calls {@link #getFriendList(String)} and appends the given name to
	 * that list and stores it back.
	 * 
	 * @return true if successfully inserted the given strToInsert to the list
	 *         of listType of the given user with the given name in the FRIENDS
	 *         table
	 */
	private static boolean insertToList(String listType, String name,
			String strToInsert) {
		try {
			String list = getFriendList(name);
			list += strToInsert + ",";
			int id = getId(name);
			Clob newList = conn.createClob();
			newList.setString(1, list);
			PreparedStatement ps = conn.prepareStatement("UPDATE " + FRIENDS
					+ " SET " + listType + " = ? WHERE id = ?");
			ps.setAsciiStream(1, newList.getAsciiStream());
			ps.setInt(2, id);
			ps.execute();
			ps.close();
			newList.free();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

	}

	/**
	 * @return the chosen list of the user with the given name.
	 */
	private static String getList(String listType, String name) {
		String list = null;

		try {
			PreparedStatement ps = conn.prepareStatement("SELECT " + listType
					+ " FROM " + FRIENDS + ", " + CLIENTS + " WHERE " + CLIENTS
					+ ".id = " + FRIENDS + ".id AND " + CLIENTS + ".name = ?");
			ps.setString(1, name);
			ResultSet result = ps.executeQuery();
			result.next();
			Clob lst = result.getClob(1);
			list = lst.getSubString(1, (int) lst.length());
			lst.free();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return list;

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
		PreparedStatement ps = conn
				.prepareStatement("CREATE TABLE "
						+ CLIENTS
						+ " (id INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), "
						+ "name VARCHAR(50) NOT NULL, " + "salt BLOB(8), "
						+ "hashedPass BLOB(20), PRIMARY KEY(id))");
		// CLIENTS
		try {
			ps.execute();
		} catch (SQLException e) {
			// DB already created
		} finally {
			ps.close();
		}

		// FRIENDS
		ps = conn
				.prepareStatement("CREATE TABLE "
						+ FRIENDS
						+ " (id INT NOT NULL PRIMARY KEY, "
						+ "friends CLOB, enemies CLOB, sentInvites CLOB, receivedInvites CLOB, "
						+ "FOREIGN KEY (id) REFERENCES " + CLIENTS + "(id))");
		try {
			ps.execute();
		} catch (SQLException e) {
			// DB already created
		} finally {
			ps.close();
		}
	}
}
