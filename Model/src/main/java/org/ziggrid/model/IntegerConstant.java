package org.ziggrid.model;

import org.ziggrid.utils.utils.PrettyPrinter;

public class IntegerConstant implements Enhancement {
	public final int value;

	public IntegerConstant(int value) {
		this.value = value;
	}

	@Override
	public void prettyPrint(PrettyPrinter pp) {
		pp.append(value);
	}

}
