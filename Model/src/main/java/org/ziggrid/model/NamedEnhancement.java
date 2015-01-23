package org.ziggrid.model;

public class NamedEnhancement {
	public final String name;
	public final Enhancement enh;

	public NamedEnhancement(String name, Enhancement enh) {
		this.name = name;
		this.enh = enh;
	}

	public FieldDefinition fieldDefinition(ObjectDefinition d) {
		String type = "number";
		FieldDefinition fd = d.getField(name);
		if (fd != null)
			type = fd.type;
		return new FieldDefinition(name, type, false);
	}
}