package org.ziggrid.model;

import org.zinutils.utils.PrettyPrinter;

public interface Decay {

	void prettyPrint(PrettyPrinter pp);

	double startFrom(Object object);

	double after(int endAt, Object object);

}
