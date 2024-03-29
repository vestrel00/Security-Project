package com.vestrel00.ssc.server.shared;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Methods used for sending and receiving byte[]. These methods are from <br>
 * <a href=
 * "http://stackoverflow.com/questions/2878867/how-to-send-an-array-of-bytes-over-a-tcp-connection-java-programming"
 * >StackOverflow</a>
 */
public class SSCStreamManager {

	/**
	 * Write the given byte[] into the given output stream.
	 * 
	 * @throws IOException
	 */
	public static void sendBytes(DataOutputStream out, byte[] bytes)
			throws IOException {
		sendBytes(out, bytes, 0, bytes.length);
	}

	/**
	 * Write the given byte[] from the byte[start] to byte[start + len] into the
	 * given output stream.
	 * 
	 * @throws IOException
	 */
	public static void sendBytes(DataOutputStream out, byte[] bytes, int start,
			int len) throws IOException {
		if (len < 0)
			throw new IllegalArgumentException("Negative length not allowed");
		if (start < 0 || start >= bytes.length)
			throw new IndexOutOfBoundsException("Out of bounds: " + start);

		out.writeInt(len);
		if (len > 0) {
			out.write(bytes, start, len);
		}
	}

	/**
	 * Blocks until there are bytes that can be read.
	 * 
	 * @return the bytes read from the input stream
	 */
	public static byte[] readBytes(DataInputStream in) throws IOException {
		// readInt() blocks!
		int len = in.readInt();
		byte[] data = new byte[len];
		if (len > 0) {
			in.readFully(data);
		}
		return data;
	}

}
