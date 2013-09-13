
package org.ziggrid.model;

import org.ziggrid.utils.utils.PrettyPrinter;

public class OpReductionWithOneField implements Reduction {
	public final String op;
	public final String eventField;

	public OpReductionWithOneField(String op, String eventField) {
		this.op = op;
		this.eventField = eventField;
	}

	@Override
	public void prettyPrint(PrettyPrinter pp) {
		pp.append(" ");
		pp.append(op);
		pp.append(" ");
		pp.append(eventField);
		pp.append(";");
	}

	@Override
	public String toString() {
		return PrettyPrinter.format(this, "prettyPrint");
	}
}
