package org.ziggrid.utils.utils;

import java.util.Map;

public class HollerithField extends HollerithItem {
	private final String field;
	public HollerithField(String field) {
		this.field = field;
		this.heading = field;
	}

	@Override
	public String toString() {
		return field;
	}

	public String apply(Map<String, String> values) {
		String val = "";
		if (field != null && values.containsKey(field))
			val = values.get(field);
		return justify.format(val, width);
	}
}
