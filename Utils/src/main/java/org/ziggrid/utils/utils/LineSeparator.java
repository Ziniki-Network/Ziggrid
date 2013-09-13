package org.ziggrid.utils.utils;

public enum LineSeparator {
	UNIX("\n"), 
	WINDOWS("\r\n"), 
	OLDMAC("\r"), 
	OTHER("");

	private final String seperatorString;

	LineSeparator(String seperatorString)
	{
		this.seperatorString = seperatorString;
	}
	
	@Override
	public String toString() {
		return seperatorString;
	}
}
