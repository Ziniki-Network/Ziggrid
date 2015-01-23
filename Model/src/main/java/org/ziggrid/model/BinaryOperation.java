package org.ziggrid.model;

import org.zinutils.utils.PrettyPrinter;

public class BinaryOperation implements Enhancement {
	public final String op;
	public final Enhancement lhs;
	public final Enhancement rhs;

	public BinaryOperation(String op, Enhancement lhs, Enhancement rhs) {
		this.op = op;
		this.lhs = lhs;
		this.rhs = rhs;
	}

	@Override
	public void prettyPrint(PrettyPrinter pp) {
		lhs.prettyPrint(pp);
		pp.append(" ");
		pp.append(op);
		pp.append(" ");
		rhs.prettyPrint(pp);
	}

}
