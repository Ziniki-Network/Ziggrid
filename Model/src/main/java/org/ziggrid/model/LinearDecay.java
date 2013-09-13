package org.ziggrid.model;

import org.ziggrid.utils.utils.PrettyPrinter;

public class LinearDecay implements Decay {
	private double expiry;

	public LinearDecay(double expiry) {
		this.expiry = expiry;
	}

	@Override
	public double startFrom(Object object) {
		if (!(object instanceof Integer))
			return 0;
		int last = (Integer) object;
		return last-expiry;
	}

	@Override
	public double after(int endAt, Object object) {
		if (!(object instanceof Integer))
			return 0;
		int when = (Integer) object;
		if (when > endAt || endAt-when > expiry)
			return 0;
		double ret = (when+expiry-endAt)/expiry;
		return ret;
	}

	@Override
	public void prettyPrint(PrettyPrinter pp) {
		pp.append("linear decay until " + expiry +";");
	}

}
