package org.ziggrid.api;

import org.zinutils.utils.StringUtil;

public final class Timestamp {
	private final int when;

	public Timestamp(int when) {
		this.when = when;
	}
	
	public int wholes() {
		return when/1000; 
	}
	
	public int millis() {
		return when%1000;
	}

	@Override
	public String toString() {
		return "["+StringUtil.digits(when/1000, 6) + "."+StringUtil.digits(when%1000, 3)+"]";
	}
}