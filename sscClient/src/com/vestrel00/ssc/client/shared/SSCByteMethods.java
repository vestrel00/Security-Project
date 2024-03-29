package com.vestrel00.ssc.client.shared;

/**
 * Simple class containing simple methods to handle byte[].
 * 
 * @author Estrellado, Vandolf
 * 
 */
public class SSCByteMethods {

	/**
	 * Concatenate a with b :<br>
	 * a[0] + a[1] .. a[a.length] + b[0] + ...b[b.length]
	 * 
	 * @return a + b
	 */
	public static byte[] concat(byte[] a, byte[] b) {
		byte[] c = new byte[a.length + b.length];
		for (int i = 0; i < a.length; i++) {
			c[i] = a[i];
		}
		for (int i = a.length, j = 0; i < a.length + b.length; i++, j++) {
			c[i] = b[j];
		}
		return c;
	}

	/**
	 * 
	 * @return true if both arrays have the same bytes stored
	 */
	public static boolean equal(byte[] a, byte[] b) {
		if (a.length != b.length)
			return false;
		for (int i = 0; i < a.length; i++) {
			if (a[i] != b[i])
				return false;
		}
		return true;
	}

}
