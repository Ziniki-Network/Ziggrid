package org.ziggrid.utils.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.ziggrid.utils.exceptions.UtilException;

public class ListMap<K, V> implements Iterable<K> {
	private final Map<K, List<V>> map;
	
	public ListMap() {
		 map = new LinkedHashMap<K, List<V>>();
	}
	
	public ListMap(Comparator<K> order) {
		map = new TreeMap<K, List<V>>(order);
	}

	public void add(K k, V v)
	{
		if (!map.containsKey(k))
			map.put(k, new ArrayList<V>());
		map.get(k).add(v);
	}

	public void add(K k, int pos, V v) {
		if (!map.containsKey(k))
			map.put(k, new ArrayList<V>());
		map.get(k).add(pos, v);
	}

	@Override
	public Iterator<K> iterator() {
		return map.keySet().iterator();
	}
	
	public List<V> get(K k)
	{
		if (!map.containsKey(k))
			throw new UtilException("There is no key '" + k + "' in " + this);
		return map.get(k);
	}

	public int totalSize() {
		int ret = 0;
		for (List<V> l : map.values())
			ret += l.size();
		return ret;
	}
	
	public int size(K k)
	{
		if (!map.containsKey(k))
			return 0;
		return map.get(k).size();
	}

	public boolean contains(K key) {
		return map.containsKey(key);
	}
	
	@Override
	public String toString() {
		return map.toString();
	}

	public Set<K> keySet() {
		return map.keySet();
	}

	public Collection<V> values() {
		Set<V> ret = new HashSet<V>();
		for (List<V> vs : map.values())
			ret.addAll(vs);
		return ret;
	}

	public List<V> removeAll(K key)
	{
		return map.remove(key);
	}
	
	public void remove(K k, V v)
	{
		if (!map.containsKey(k))
			return;
		List<V> list = map.get(k);
		list.remove(v);
		if (list.isEmpty())
			map.remove(k);
	}

	public V removeFirst(K key) {
		if (!map.containsKey(key))
			throw new UtilException("There is no key " + key);
		List<V> list = map.get(key);
		V ret = list.remove(0);
		if (list.isEmpty())
			map.remove(key);
		return ret;
	}
	
	public Set<Entry<K, List<V>>> entrySet() {
		return map.entrySet();
	}

	public void extract(Collection<V> ret, K k, int offset, int count) {
		if (!map.containsKey(k))
			return;
		List<V> contents = map.get(k);
		for (int i=offset;i<count && contents.size() > i;i++)
		{
			ret.add(contents.get(i));
		}
	}

	public void take(Collection<V> ret, K k, int count) {
		if (!map.containsKey(k))
			return;
		List<V> contents = map.get(k);
		for (int i=0;i<count && contents.size() > 0;i++)
		{
			ret.add(contents.remove(0));
		}
	}

	public void ensure(K k) {
		if (!map.containsKey(k))
			map.put(k, new ArrayList<V>());
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public void clear() {
		map.clear();
	}
}
