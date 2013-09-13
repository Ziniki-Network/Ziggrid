package org.ziggrid.model;

import org.ziggrid.utils.utils.PrettyPrinter;

public class MatchField {
	public final String summaryField;
	public final String eventField;

	public MatchField(String summaryField, String eventField) {
		this.summaryField = summaryField;
		this.eventField = eventField;
	}

	public void prettyPrint(PrettyPrinter pp) {
		pp.append(summaryField);
		pp.append(" == ");
		pp.append(eventField);
		pp.append(";");
		pp.requireNewline();
	}

}
