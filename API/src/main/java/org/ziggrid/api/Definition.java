package org.ziggrid.api;

import org.zinutils.utils.PrettyPrinter;

public interface Definition {
	void prettyPrint(PrettyPrinter pp);

	String context();
}
