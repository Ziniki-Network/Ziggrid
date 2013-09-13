package org.ziggrid.model;

import org.ziggrid.utils.utils.PrettyPrinter;

public class DoubleConstant implements Enhancement {
	public final double value;

	public DoubleConstant(double value) {
		this.value = value;
	}

	@Override
	public void prettyPrint(PrettyPrinter pp) {
		pp.append(value);
	}

}
