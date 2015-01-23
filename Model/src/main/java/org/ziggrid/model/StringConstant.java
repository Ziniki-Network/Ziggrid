package org.ziggrid.model;

import org.zinutils.utils.PrettyPrinter;

public class StringConstant implements Enhancement {
	public final String value;

	public StringConstant(String s) {
		this.value = s;
	}

	@Override
	public void prettyPrint(PrettyPrinter pp) {
		pp.append('"');
		pp.append(value);
		pp.append('"');
	}

}
