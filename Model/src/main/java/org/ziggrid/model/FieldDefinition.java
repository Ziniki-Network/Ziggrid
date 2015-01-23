package org.ziggrid.model;

import java.util.ArrayList;
import java.util.List;

import org.zinutils.utils.PrettyPrinter;

public class FieldDefinition {
	public final String name;
	public final String type;
	public final boolean isKey;
	private final List<String> params = new ArrayList<String>();

	public FieldDefinition(String name, String type, boolean isKey) {
		this.name = name;
		this.type = type;
		this.isKey = isKey;
	}

	public void addParam(String s) {
		params.add(s);
	}
	
	public void prettyPrint(PrettyPrinter pp) {
		if (isKey)
			pp.append("key ");
		pp.append(type);
		pp.append(" ");
		pp.append(name);
		pp.append(";");
		pp.requireNewline();
	}

	public String param(int i) {
		return params.get(i);
	}
	
	@Override
	public String toString() {
		return name + ":"+type+(isKey?"*":"");
	}
}
