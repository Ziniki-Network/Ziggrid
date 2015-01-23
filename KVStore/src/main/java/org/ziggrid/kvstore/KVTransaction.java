package org.ziggrid.kvstore;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.ziggrid.api.StoreableObject;

public interface KVTransaction {
	// Strubg map related requests
	boolean containsValue(String string);
	String getString(String id);
	Object getValue(String string);
	void put(String key, String str);
	void putValue(String string, Object val);
	Map<String, String> getKeyRangePrefixed(String string, int top, boolean wantAscending);
	void deleteKey(String key);

	// Object-related requests
	boolean contains(String string);
	StoreableObject get(String id);
	List<StoreableObject> getAllPrefixed(String string);
	List<StoreableObject> getAfter(String afterId, String lastId);
	void require(String id, String ofType);
	void put(byte[] itemId, String key, Object value);
	void put(String id, StoreableObject obj);
	void setField(String id, String key, Object value);
	void deleteObject(String id);
	void delete(byte[] qItem);

	Collection<StoreableObject> updatedObjects();

	void commit();
	void rollback();
	boolean isRetryable();

	Sender getSender();
}
