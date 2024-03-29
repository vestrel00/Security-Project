package com.vestrel00.ssc.server.shared;

/**
 * Simple methods for handling strings. Not static methods to be thread safe.
 * Different threads accessing using the same StrinBuilger is dangerous.<br>
 * <b>List format is <element>,<element>,...</b>
 * 
 * @author Estrellado, Vandolf
 * 
 */
public class SSCStringMethods {

	private StringBuilder builder;
	private static final String comma = ",";

	/**
	 * Initialize class variables.
	 */
	public SSCStringMethods() {
		builder = new StringBuilder();
	}

	/**
	 * @return true if the given name is in the list.
	 */
	public boolean isInList(String list, String name) {
		int i = 0, j; // keep index of next comma
		builder.delete(0, builder.length());
		builder.append(list);
		// -1 when no more comma
		while ((j = builder.indexOf(comma, i)) != -1) {
			if (builder.substring(i, j).contentEquals(name))
				return true;
			i = j + 1;
		}
		return false;

	}

	/**
	 * Assumes that the given name is in the list
	 */
	public String removeFromList(String list, String name) {
		builder.delete(0, builder.length());
		builder.append(list);
		builder.delete(builder.indexOf(name), name.length() + 1);
		return builder.toString();
	}
}
