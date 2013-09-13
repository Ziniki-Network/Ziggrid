package org.ziggrid.utils.collections;

import java.util.Enumeration;
import java.util.Iterator;

public class IteratorEnumerator<T> implements Enumeration<T> {

	private final Iterator<T> iterator;

	public IteratorEnumerator(Iterator<T> iterator) {
		this.iterator = iterator;
	}

	@Override
	public boolean hasMoreElements() {
		return iterator.hasNext();
	}

	@Override
	public T nextElement() {
		return iterator.next();
	}

}
