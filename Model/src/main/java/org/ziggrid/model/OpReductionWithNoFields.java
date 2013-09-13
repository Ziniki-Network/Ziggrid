
package org.ziggrid.model;

import org.ziggrid.utils.utils.PrettyPrinter;

public class OpReductionWithNoFields implements Reduction {
	public final String op;

	public OpReductionWithNoFields(String op) {
		this.op = op;
	}

	@Override
	public void prettyPrint(PrettyPrinter pp) {
		pp.append(" ");
		pp.append(op);
		pp.append(";");
	}

	@Override
	public String toString() {
		return PrettyPrinter.format(this, "prettyPrint");
	}
}
