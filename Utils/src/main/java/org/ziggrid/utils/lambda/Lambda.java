package org.ziggrid.utils.lambda;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Lambda {
	public static <TR, T1> Set<TR> map(FuncR1<TR, T1> func, Set<T1> input)
	{
		Set<TR> ret = new HashSet<TR>();
		for (T1 in : input)
			ret.add(func.apply(in));
		return ret;
	}

	public static <TR, T1> List<TR> map(FuncR1<TR, T1> func, List<T1> input)
	{
		List<TR> ret = new ArrayList<TR>();
		for (T1 in : input)
			ret.add(func.apply(in));
		return ret;
	}

	public static <TR, T1> Iterator<TR> map(FuncR1<TR, T1> func, Iterator<T1> input) {
		return new MapIterator<TR,T1>(func, input);
	}

	public static <T1> Iterator<T1> filter(FuncR1<Boolean, T1> func, Iterator<T1> iterator) {
		return new FilterIterator<T1>(func, iterator);
	}

	public static <T1, T2> Map<T2, T1> createMapFromCollection(FuncR1<T2, T1> func, Collection<? extends T1> coll) {
		Map<T2, T1> ret = new HashMap<T2, T1>();
		for (T1 t : coll)
		{
			ret.put(func.apply(t), t);
		}
		return ret;
	}
}
