package org.ziggrid.utils.collections;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ziggrid.utils.exceptions.UtilException;

public class CollectionUtils {
	public static <T> List<T> setToList(Set<T> in, Comparator<T> ordering)
	{
		List<T> ret = new ArrayList<T>();
		ret.addAll(in);
		Collections.sort(ret, ordering);
		return ret;
	}
	
	public static <T> T any(Iterable<T> coll)
	{
		Iterator<T> it = coll.iterator();
		if (!it.hasNext())
			throw new UtilException("Any requires at least one element to function");
		return it.next();
	}

	public static <T> T nth(Iterable<T> coll, int which) {
		Iterator<T> it = coll.iterator();
		T ret = null;
		while (which-- >= 0)
			ret = it.next();
		return ret;
	}

	public static <T> List<T> listOf(T... items) {
		List<T> ret = new ArrayList<T>();
		for (T x : items)
			ret.add(x);
		return ret;
	}

	public static <T> Set<T> setOf(T... items) {
		Set<T> ret = new HashSet<T>();
		for (T x : items)
			ret.add(x);
		return ret;
	}

	public static <T> ArrayList<T> array(Iterator<T> it) {
		ArrayList<T> ret = new ArrayList<T>();
		while (it.hasNext())
			ret.add(it.next());
		return ret;
	}

	@SuppressWarnings("unchecked")
	public static <T> Map<String, T> map(Object... args) {
		Map<String, T> ret = new HashMap<String, T>();
		for (int i=0;i+1<args.length;i+=2)
			ret.put((String) args[i], (T)args[i+1]);
		return ret;
	}

	public static <T> T[] arrayAppend(Class<T> cls, T[] arr, T... append) {
		@SuppressWarnings("unchecked")
		T[] ret = (T[]) Array.newInstance(cls, arr.length + append.length);
		for (int i=0;i<arr.length;i++)
			ret[i] = arr[i];
		for (int i=0;i<append.length;i++)
			ret[i+arr.length] = append[i];
		return ret;
	}

	public static <T> Iterable<T> iterableOf(final Iterator<T> keys) {
		return new Iterable<T>() {
			public Iterator<T> iterator() {
				return keys;
			}
			
		};
	}
}
