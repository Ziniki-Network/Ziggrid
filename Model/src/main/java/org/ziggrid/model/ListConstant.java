package org.ziggrid.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.ziggrid.utils.utils.PrettyPrinter;

public class ListConstant implements Enhancement, Iterable<Object> {
	public final List<Object> values = new ArrayList<Object>();
	
	public void add(Object o) {
		values.add(o);
	}

	@Override
	public void prettyPrint(PrettyPrinter pp) {
		pp.append(values);
	}

	@Override
	public Iterator<Object> iterator() {
		return values.iterator();
	}

}
