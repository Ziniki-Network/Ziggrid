package org.ziggrid.model;

import org.zinutils.utils.PrettyPrinter;

public class MatchField {
	public final String summaryField;
	public final String eventField;

	public MatchField(String summaryField, String eventField) {
		this.summaryField = summaryField;
		this.eventField = eventField;
	}

	public void prettyPrint(PrettyPrinter pp) {
		pp.append(toString());
		pp.append(";");
		pp.requireNewline();
	}

	public String toString() {
		return summaryField + " == " + eventField;
	}
}
