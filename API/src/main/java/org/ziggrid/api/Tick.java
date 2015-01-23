package org.ziggrid.api;

import org.zinutils.utils.StringUtil;

public class Tick {
	public final int when;

	public Tick(int when) {
		this.when = when;
	}

	@Override
	public String toString() {
		return "["+StringUtil.digits(when/1000, 6) + ".---"+"]";
	}
}