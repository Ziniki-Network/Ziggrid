package org.ziggrid.utils.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.jasypt.util.text.BasicTextEncryptor;

import org.ziggrid.utils.exceptions.UtilException;

public class Crypto {
	
	public static String hash(String str)
	{
		return StringUtil.hex(computeHash(str));
	}

	public static byte[] computeHash(String str) {
		try {
			MessageDigest d = MessageDigest.getInstance("SHA-1");
			d.reset();
			d.update(str.getBytes());
			return d.digest();
		} catch (NoSuchAlgorithmException e) {
			throw UtilException.wrap(e);
		}
	}
	
	public static String encrypt(String password, String text)
	{
		BasicTextEncryptor bte = new BasicTextEncryptor();
		bte.setPassword(password);
		return bte.encrypt(text);
	}
	
	public static String decrypt(String password, String text)
	{
		BasicTextEncryptor bte = new BasicTextEncryptor();
		bte.setPassword(password);
		return bte.decrypt(text);
	}
	
	public static void main(String[] argv) {
		for (String s : argv)
			System.out.println(s + ": " + hash(s));
	}
}
