package org.ziggrid.utils.collections;

import org.ziggrid.utils.exceptions.UtilException;

public class CircularList<T> {
	private final CircularList<T> next;
	private final int size;
	private T entry;
	
	public CircularList(int size) {
		if (size < 1)
			throw new UtilException("Cannot have a CircularList with < 1 elements");
		CircularList<T> v = this;
		for (int i=0;i<size;i++)
			v = new CircularList<T>(v, size);
		next = v;
		this.size = size;
	}

	private CircularList(CircularList<T> next, int size) {
		this.next = next;
		this.size = size;
	}

	public int size() {
		return size;
	}
	
	public void set(T e) {
		this.entry = e;
	}
	
	public T get() {
		return this.entry;
	}
	
	public CircularList<T> getNext() {
		return next;
	}
}
