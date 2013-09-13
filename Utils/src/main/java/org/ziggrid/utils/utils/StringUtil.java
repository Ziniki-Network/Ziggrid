package org.ziggrid.utils.utils;

import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ziggrid.utils.exceptions.UtilException;

public class StringUtil {

	public static String concatVertically(List<String> errors) {
		StringBuilder sb = new StringBuilder();
		for (String s : errors)
		{
			sb.append(s);
			sb.append("\n");
		}
		return sb.toString();
	}

	// TODO: this needs a proper implementation
	public static boolean globMatch(String pattern, String string) {
		if (pattern.startsWith("*"))
			return string.endsWith(pattern.substring(1));
		else if (pattern.endsWith("*"))
			return string.startsWith(pattern.substring(0, pattern.length()-1));
		else
			return string.equals(pattern);
	}

	public static String concat(String...  args) {
		StringBuilder sb = new StringBuilder();
		for (String s : args)
			sb.append(s);
		return sb.toString();
	}

	public static String concatSep(String separator, List<String> args) {
		StringBuilder sb = new StringBuilder();
		String sep = "";
		for (String s : args) {
			sb.append(sep);
			sb.append(s);
			sep = separator;
		}
		return sb.toString();
	}

	public static String rjdigits(int quant, int nd) {
		StringBuilder sb = new StringBuilder();
		sb.append(quant);
		if (sb.length() > nd)
			sb.delete(0, sb.length()-nd);
		while (sb.length() < nd)
			sb.insert(0, " ");
		return sb.toString();
	}

	public static String digits(int quant, int nd) {
		StringBuilder sb = new StringBuilder();
		sb.append(quant);
		if (sb.length() > nd)
			sb.delete(0, sb.length()-nd);
		while (sb.length() < nd)
			sb.insert(0, "0");
		return sb.toString();
	}

	public static String hex(int quant, int nd) {
		StringBuilder sb = new StringBuilder();
		hex(sb, 0, quant, nd);
		return sb.toString();
	}

	private static void hex(StringBuilder sb, int off, int quant, int nd) {
		sb.append(Integer.toHexString(quant).toUpperCase());
		if (sb.length() > off+nd)
			sb.delete(off, sb.length()-nd);
		while (sb.length() < off+nd)
			sb.insert(off, "0");
	}
	  
	public static String hex(byte[] b) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < b.length; i++)
			hex(sb, i*2, b[i] & 0xff, 2);
		return sb.toString();
	}

	public static String hex(byte[] b, int off, int len) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < len; i++)
			hex(sb, i*2, b[off+i] & 0xff, 2);
		return sb.toString();
	}

	public static char uniqueLetter(int idx) {
		if (idx < 0)
			return '-';
		else if (idx < 10)
			return (char) ('0' + idx);
		else if (idx < 36)
			return (char) ('A' + idx-10);
		else if (idx < 62)
			return (char) ('a' + idx-36);
		else if (idx < 72)
			return (char) ('!' + idx-72);
		else
			return '+';
	}

	public static String capitalize(String s)
	{
		if (s == null)
			return null;
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	public static String decapitalize(String s) {
		return Character.toLowerCase(s.charAt(0)) + s.substring(1);
	}

	public static Iterable<String> lines(String stderr) {
		try
		{
			List<String> ret = new ArrayList<String>();
			LineNumberReader lnr = new LineNumberReader(new StringReader(stderr));
			String s;
			while ((s = lnr.readLine()) != null)
				ret.add(s);
			return ret;
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}

	public static byte[] fromHex(String s, int len) {
		if (len != 0 && s.length() != 2*len)
			throw new UtilException("Invalid string length: must be " + 2*len + " not " + s.length());
		byte[] ret = new byte[len];
		for (int i=0;i<len;i++)
			ret[i] = (byte) Integer.parseInt(s.substring(2*i,2*i+2), 16);
		return ret;
	}

	public static boolean isHexDigit(char ch) {
		return Character.isDigit(ch) || (ch >='a' && ch <='f') || (ch >='A' && ch <='F');
	}

	public static String truncate(String tmp, int columns) {
		if (tmp == null)
			return "";
		if (tmp.length() <= columns)
			return tmp;
		return tmp.substring(0, columns);
	}

	public static String join(Collection<? extends Object> object) {
		return join(object, " ");
	}
	
	public static String join(Collection<? extends Object> object, String separator) {
		StringBuilder sb = new StringBuilder();
		String sep = "";
		for (Object o : object) {
			sb.append(sep);
			sb.append(o);
			sep = separator;
		}
		return sb.toString();
	}

	public static String defaultValue(String value, String def) {
		if (value != null)
			return value;
		return def;
	}
}
