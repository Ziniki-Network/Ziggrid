package org.ziggrid.model;

import org.ziggrid.utils.utils.PrettyPrinter;

public interface Definition {
	void prettyPrint(PrettyPrinter pp);

	String context();
}
