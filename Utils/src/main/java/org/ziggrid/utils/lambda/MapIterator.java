package org.ziggrid.utils.lambda;

import java.util.Iterator;

public class MapIterator<TR, T1> implements Iterator<TR> {

	private final FuncR1<TR, T1> func;
	private final Iterator<T1> input;

	public MapIterator(FuncR1<TR, T1> func, Iterator<T1> input) {
		this.func = func;
		this.input = input;
	}

	@Override
	public boolean hasNext() {
		return input.hasNext();
	}

	@Override
	public TR next() {
		return func.apply(input.next());
	}

	@Override
	public void remove() {
		input.remove();
	}

}
