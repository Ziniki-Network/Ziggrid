package org.ziggrid.model;

import org.zinutils.utils.PrettyPrinter;

public class GroupingOperation implements Enhancement {
	public final Enhancement basedOn;
	public final Enhancement dividers;
	public final Enhancement moreThan;

	public GroupingOperation(Enhancement basedOn, Enhancement dividers, Enhancement moreThan) {
		this.basedOn = basedOn;
		this.dividers = dividers;
		this.moreThan = moreThan;
	}

	@Override
	public void prettyPrint(PrettyPrinter pp) {
		pp.append("group value");
		pp.indentMore();
		pp.indentMore();
		basedOn.prettyPrint(pp);
		pp.indentLess();
		pp.append("divided by ");
		dividers.prettyPrint(pp);
		pp.requireNewline();
		pp.append("with max value ");
		moreThan.prettyPrint(pp);
		pp.indentLess();
	}

}
