package org.ziggrid.kvstore.memory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.ziggrid.api.StoreableObject;
import org.ziggrid.kvstore.KVTransaction;
import org.ziggrid.kvstore.Sender;
import org.zinutils.exceptions.NotImplementedException;
import org.zinutils.exceptions.UtilException;

// TODO: we should support some level of transactional consistency
public class MemoryTx implements KVTransaction {
	private final MemoryDatabase mdb;
	private final Set<StoreableObject> updatedObjects = new HashSet<StoreableObject>();
	private final MemorySender sender;

	public MemoryTx(MemoryDatabase mdb, MemorySender sender) {
		this.mdb = mdb;
		this.sender = sender;
	}
	
	@Override
	public boolean isRetryable() {
		throw new NotImplementedException();
	}

	public boolean containsValue(String id) {
		synchronized (mdb.contents) {
			return mdb.contents.containsKey(id) && !(mdb.contents.get(id) instanceof StoreableObject);
		}
	}

	public String getString(String id) {
		synchronized (mdb.contents) {
			if (!mdb.contents.containsKey(id))
				throw new UtilException("There is no key " + id);
			Object ret = mdb.contents.get(id);
			if (ret instanceof String)
				return (String) ret;
			throw new UtilException("Cannot convert " + id + " to string");
		}
	}

	@Override
	public Object getValue(String id) {
		synchronized (mdb.contents) {
			if (!mdb.contents.containsKey(id))
				throw new UtilException("There is no key " + id);
			return mdb.contents.get(id);
		}
	}

	public boolean contains(String id) {
		synchronized (mdb.contents) {
			return mdb.contents.containsKey(id) && mdb.contents.get(id) instanceof StoreableObject;
		}
	}

	public StoreableObject get(String id) {
		synchronized (mdb.contents) {
			if (!mdb.contents.containsKey(id))
				throw new UtilException("There is no key " + id);
			Object ret = mdb.contents.get(id);
			if (ret instanceof StoreableObject)
				return (StoreableObject) ret;
			throw new UtilException("Cannot convert " + id + " to storeableObject");
		}
	}

	@Override
	public List<StoreableObject> getAfter(String afterId, String lastId) {
		List<StoreableObject> ret = new ArrayList<StoreableObject>();
		synchronized (mdb.contents) {
			Entry<String, Object> entry = mdb.contents.ceilingEntry(afterId);
			while (entry != null && entry.getKey().compareTo(lastId) < 0) {
				ret.add((StoreableObject) entry.getValue());
				entry = mdb.contents.higherEntry(entry.getKey());
			}
		}
		return ret;
	}

	@Override
	public List<StoreableObject> getAllPrefixed(String prefix) {
		List<StoreableObject> ret = new ArrayList<StoreableObject>();
		synchronized (mdb.contents) {
			Entry<String, Object> entry = mdb.contents.ceilingEntry(prefix);
			while (entry != null && entry.getKey().startsWith(prefix)) {
				ret.add((StoreableObject) entry.getValue());
				entry = mdb.contents.higherEntry(entry.getKey());
			}
		}
		return ret;
	}

	@Override
	public Map<String, String> getKeyRangePrefixed(String string, int top, boolean wantAscending) {
		Map<String, String> ret = new LinkedHashMap<String, String>();
		synchronized (mdb.contents) {
			if (wantAscending) {
				Entry<String, Object> entry = mdb.contents.ceilingEntry(string);
				while (top-- > 0 && entry != null && entry.getKey().startsWith(string)) {
					ret.put(entry.getKey(), (String) entry.getValue());
					entry = mdb.contents.higherEntry(entry.getKey());
				}
			} else {
				Entry<String, Object> entry = mdb.contents.ceilingEntry(string);
				while (entry != null && entry.getKey().startsWith(string)) {
					entry = mdb.contents.higherEntry(entry.getKey());
				}
				if (entry == null)
					entry = mdb.contents.lastEntry();
				else
					entry = mdb.contents.lowerEntry(entry.getKey());
				while (top-- > 0 && entry != null && entry.getKey().startsWith(string)) {
					try {
						ret.put(entry.getKey(), (String) entry.getValue());
						entry = mdb.contents.lowerEntry(entry.getKey());
					} catch (ClassCastException ex) {
						ex.printStackTrace();
						System.out.println("Broken");
					}
				}
			}
		}
		return ret;
	}

	@Override
	public void require(String id, String ofType) {
		synchronized (mdb.contents) {
			if (!mdb.contents.containsKey(id))
				put(id, new StoreableObject(ofType, id));
		}
	}
	
	public void put(String id, StoreableObject obj) {
		synchronized (mdb.contents) {
			mdb.contents.put(id, obj);
			updatedObjects.add(obj);
		}
	}
	
	public void put(String id, String str) {
		synchronized (mdb.contents) {
			mdb.contents.put(id, str);
		}
	}
	
	@Override
	public void putValue(String string, Object val) {
		synchronized (mdb.contents) {
			mdb.contents.put(string, val);
		}
	}

	@Override
	public void put(byte[] itemId, String key, Object value) {
		throw new NotImplementedException("I think this is only required for queuing");
	}

	public void setField(String id, String field, Object value) {
		StoreableObject obj;
		
		if (mdb.contents.containsKey(id)) {
			obj = get(id);
		} else {
			obj = new StoreableObject("_mapped", id);
			mdb.contents.put(id, obj);
		}
		
		obj.set(field, value);
	}
	
	public void deleteObject(String id) {
		if (mdb.contents.containsKey(id)) {
			mdb.contents.remove(id);
		}
	}
	
	public void deleteKey(String id) {
		if (mdb.contents.containsKey(id)) {
			mdb.contents.remove(id);
		}
	}
	
	@Override
	public void delete(byte[] qItem) {
		throw new NotImplementedException();
	}

	@Override
	public Collection<StoreableObject> updatedObjects() {
		return updatedObjects;
	}
	
	public void commit() {
		
	}

	@Override
	public void rollback() {
		throw new UtilException("Transaction was forced to roll back, which we can't handle");
	}

	@Override
	public Sender getSender() {
		return sender;
	}

}
