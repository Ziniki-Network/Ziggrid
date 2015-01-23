package org.ziggrid.model;

import org.zinutils.utils.PrettyPrinter;

public class FieldEnhancement implements Enhancement {
	public final String field;

	public FieldEnhancement(String field) {
		this.field = field;
	}

	@Override
	public void prettyPrint(PrettyPrinter pp) {
		pp.append(field);
	}

	@Override
	public String toString() {
		return "Field[" + field + "]";
	}
}
