package org.ziggrid.model;

import org.zinutils.utils.PrettyPrinter;

public class IfElseOperation implements Enhancement {
	public final Enhancement test;
	public final Enhancement ifTrue;
	public final Enhancement ifFalse;

	public IfElseOperation(Enhancement test, Enhancement ifTrue, Enhancement ifFalse) {
		this.test = test;
		this.ifTrue = ifTrue;
		this.ifFalse = ifFalse;
	}

	@Override
	public void prettyPrint(PrettyPrinter pp) {
		pp.append("ifelse ");
		test.prettyPrint(pp);
		pp.indentMore();
		pp.append("true  => ");
		ifTrue.prettyPrint(pp);
		pp.requireNewline();
		pp.append("false => ");
		ifFalse.prettyPrint(pp);
		pp.indentLess();
	}
}
