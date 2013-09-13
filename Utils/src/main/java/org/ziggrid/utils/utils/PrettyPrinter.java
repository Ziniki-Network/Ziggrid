package org.ziggrid.utils.utils;

import java.io.PrintStream;

import org.ziggrid.utils.reflection.Reflection;

public class PrettyPrinter {
	private StringBuilder sb = new StringBuilder();
	private int indWidth = 4;
	private int levels = 0;
	private Hollerith hollerith;
	private boolean nlAtEnd = true;
	
	public static String format(Object o, String method) {
		PrettyPrinter pp = new PrettyPrinter();
		Reflection.call(o, method, pp);
		return pp.toString();
	}
	
	public static void print(PrintStream stream, Object o, String method) {
		stream.print(format(o, method));
	}
	
	/** Number of spaces to indent each level.  The default is 4.
	 * 
	 * @param ind the number of spaces for each indent
	 */
	public void indentWidth(int ind)
	{
		indWidth = ind;
	}
	
	public void setNlAtEnd(boolean nl) {
		nlAtEnd = nl;
	}

	/** Indent one level more */
	public void indentMore()
	{
		requireNewline();
		levels++;
	}
	
	/** Indent one level less */
	public void indentLess()
	{
		requireNewline();
		levels--;
	}
	
	public int getIndent() {
		return levels;
	}

	/** Test if the cursor is currently at the start of a line */
	public boolean atLineStart() {
		return sb.length() == 0 || sb.charAt(sb.length()-1) == '\n';
	}
	
	/** Make sure that the cursor is at the start of a line, but don't insert duplicate newlines. */
	public void requireNewline()
	{
		if (hollerith != null)
			sb.append(hollerith.format());
		if (atLineStart())
			return;
		sb.append('\n');
	}

	/** Append an object's toString representation
	 * 
	 * @param o the object to convert and append
	 */
	public void append(Object o)
	{
		if (atLineStart())
			for (int i=0;i<levels*indWidth;i++)
				sb.append(" ");
		sb.append(o);
	}
	
	/** Break up the input into lines, then add each line back in again, indenting as you go.
	 * All lines will have the same initial indent, but if they are indented themselves, will vary.
	 *
	 * @param s the composite string to break up
	 */
	public void appendIndented(String s)
	{
		int from = 0;
		while (from < s.length())
		{
			int idx = s.indexOf('\n', from);
			if (idx == -1)
				idx = s.length();

			requireNewline();
			append(s.substring(from, idx));
			from = idx+1;
		}
		requireNewline();
	}
	
	@Override
	public String toString() {
		if (nlAtEnd)
			requireNewline();
		return sb.toString();
	}

	public Hollerith hollerith(HollerithFormat fmt)
	{
		requireNewline();
		hollerith = new Hollerith(fmt);
		return hollerith; 
	}

	public void appendIndentedBlock(PrettyPrintable obj) {
		if (obj == null)
			return;
		indentMore();
		obj.prettyPrint(this);
		indentLess();
	}

	public int length() {
		return sb.length();
	}

	public boolean hasWhitespace() {
		return atLineStart() || Character.isWhitespace(sb.charAt(sb.length()-1));
	}

	public char lastChar() {
		return sb.charAt(sb.length()-1);
	}
}
