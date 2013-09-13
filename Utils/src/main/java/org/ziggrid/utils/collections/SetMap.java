package org.ziggrid.utils.collections;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.ziggrid.utils.exceptions.UtilException;

public class SetMap<K, V> implements Iterable<K> {
	private Map<K, Set<V>> map = new HashMap<K, Set<V>>();
	
	public void add(K k, V v)
	{
		if (!map.containsKey(k))
			map.put(k, new HashSet<V>());
		map.get(k).add(v);
	}

	@Override
	public Iterator<K> iterator() {
		return map.keySet().iterator();
	}
	
	public Iterable<V> values() {
		Set<V> accum = new HashSet<V>();
		for (Set<V> v : map.values())
			accum.addAll(v);
		return accum;
	}
	
	public Set<V> get(K k)
	{
		if (!map.containsKey(k))
			throw new UtilException("There is no key '" + k + "' in " + this);
		return map.get(k);
	}

	public boolean contains(K key) {
		return map.containsKey(key);
	}
	
	@Override
	public String toString() {
		return map.toString();
	}

	public void addAll(K k, Iterable<V> vals) {
		for (V v : vals)
			add(k,  v);
	}

	public int totalSize() {
		int ret = 0;
		for (Set<V> x : map.values())
			ret += x.size();
		return ret;
	}

	public int size(K k)
	{
		if (!map.containsKey(k))
			return 0;
		return map.get(k).size();
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public Set<K> keySet() {
		return map.keySet();
	}
}
