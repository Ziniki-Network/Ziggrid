package org.ziggrid.model;

import org.zinutils.utils.PrettyPrinter;

public class StringContainsOp implements Enhancement {
	public final String field;
	public final String text;

	public StringContainsOp(String field, String text) {
		this.field = field;
		this.text = text;
	}

	@Override
	public void prettyPrint(PrettyPrinter pp) {
		pp.append(field);
		pp.append(" contains ");
		pp.append('"');
		pp.append(text);
		pp.append('"');
	}
}
