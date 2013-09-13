package org.ziggrid.utils.utils;

import java.util.HashMap;
import java.util.Map;

public class Hollerith {

	private final HollerithFormat fmt;
	private final Map<String, String> fields = new HashMap<String, String>();

	public Hollerith(HollerithFormat fmt) {
		this.fmt = fmt;
	}

	public String format() {
		return fmt.assemble(fields);
	}

	public Hollerith set(String field, String value) {
		if (fmt.hasField(field))
			fields.put(field, value);
		return this;
	}

	public Hollerith setToString(String field, Object value) {
		if (fmt.hasField(field) && value != null)
			fields.put(field, value.toString());
		return this;
	}

}
