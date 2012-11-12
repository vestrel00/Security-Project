package com.vestrel00.ssc.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SSCStreamManager {

	public static void sendBytes(DataOutputStream out, byte[] bytes)
			throws IOException {
		sendBytes(out, bytes, 0, bytes.length);
	}

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