package org.ziggrid.utils.lambda;

import java.util.Iterator;

import org.ziggrid.utils.exceptions.UtilException;

public class FilterIterator<T1> implements Iterator<T1> {

	private final FuncR1<Boolean, T1> func;
	private final Iterator<T1> input;
	private T1 nextResult = null;

	public FilterIterator(FuncR1<Boolean, T1> func, Iterator<T1> input) {
		this.func = func;
		this.input = input;
	}

	@Override
	public boolean hasNext() {
		if (nextResult != null)
			return true;
		while (input.hasNext())
		{
			nextResult = input.next();
			if (func.apply(nextResult))
				return true;
		}
		return false;
	}

	@Override
	public T1 next() {
		T1 ret = nextResult;
		nextResult = null;
		return ret;
	}

	@Override
	public void remove() {
		throw new UtilException("remove() is not supported");
	}

}
