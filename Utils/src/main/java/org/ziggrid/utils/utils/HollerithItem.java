package org.ziggrid.utils.utils;

import java.util.Map;

public abstract class HollerithItem {

	protected Justification justify = Justification.LEFT;
	protected int width = -1;
	protected String heading;

	public HollerithItem setWidth(int i) {
		width  = i;
		return this;
	}

	public HollerithItem setJustification(Justification j) {
		justify = j;
		return this;
	}

	public String getHeading() {
		return heading;
	}

	public abstract String apply(Map<String, String> values);
}
