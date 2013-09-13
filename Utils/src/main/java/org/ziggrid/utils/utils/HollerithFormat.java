package org.ziggrid.utils.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ziggrid.utils.exceptions.UtilException;

public class HollerithFormat {
	private List<HollerithItem> order = new ArrayList<HollerithItem>();
	private Map<String,HollerithField> fields = new HashMap<String, HollerithField>();

	public HollerithField addField(String field) {
		if (hasField(field))
			throw new UtilException("The field " + field + " cannot be defined twice");
		HollerithField f = new HollerithField(field);
		fields.put(field, f);
		order.add(f);
		return f;
	}

	public void addPadding(int i) {
		order.add(new HollerithText("").setWidth(i));
	}

	public void addText(String string) {
		order.add(new HollerithText(string).setWidth(string.length()));
	}

	public void titles(Hollerith hollerith) {
		for (Entry<String, HollerithField> i : fields.entrySet())
		{
			hollerith.set(i.getKey(), i.getValue().getHeading());
		}
	}

	public boolean hasField(String field) {
		return fields.containsKey(field);
	}

	@Override
	public String toString() {
		// I think what I really want is a functional composition stringbuilder
		StringBuilder ret = new StringBuilder();
		ret.append("HollerithFormat{");
		for (HollerithItem e : order)
		{
			ret.append(e+",");
		}
		ret.append("]");
		return ret.toString();
	}

	public String assemble(Map<String, String> values) {
		StringBuilder ret = new StringBuilder();
		for (HollerithItem f : order)
			ret.append(f.apply(values));
		return ret.toString();
	}
}
