package com.vestrel00.ssc.server.shared;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

/**
 * Simple file saving protocol taken from <a
 * href="http://www.javamex.com/tutorials/cryptography/rsa_encryption.shtml">
 * RSA Encryption tutorial</a>
 */
public class SSCFileManager {

	public static void saveToFile(String fileName, BigInteger mod,
			BigInteger exp) throws FileNotFoundException, IOException {
		ObjectOutputStream oout = new ObjectOutputStream(
				new BufferedOutputStream(new FileOutputStream(fileName)));
		try {
			oout.writeObject(mod);
			oout.writeObject(exp);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// make sure out stream is closed
			oout.close();
		}
	}

	public static PublicKey readPublicFromFile(String keyFileName)
			throws FileNotFoundException, IOException {
		ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(
				new FileInputStream(keyFileName)));
		try {
			BigInteger mod = (BigInteger) oin.readObject();
			BigInteger exp = (BigInteger) oin.readObject();
			RSAPublicKeySpec keySpec = new RSAPublicKeySpec(mod, exp);
			KeyFactory fact = KeyFactory.getInstance("RSA");
			return fact.generatePublic(keySpec);
		} catch (IOException | ClassNotFoundException
				| NoSuchAlgorithmException | InvalidKeySpecException e) {
			e.printStackTrace();
		} finally {
			oin.close();
		}
		return null;
	}

	public static PrivateKey readPrivateFromFile(String keyFileName)
			throws FileNotFoundException, IOException {
		ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(
				new FileInputStream(keyFileName)));
		try {
			BigInteger mod = (BigInteger) oin.readObject();
			BigInteger exp = (BigInteger) oin.readObject();
			RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(mod, exp);
			KeyFactory fact = KeyFactory.getInstance("RSA");
			return fact.generatePrivate(keySpec);
		} catch (IOException | ClassNotFoundException
				| NoSuchAlgorithmException | InvalidKeySpecException e) {
			e.printStackTrace();
		} finally {
			oin.close();
		}
		return null;
	}

}
