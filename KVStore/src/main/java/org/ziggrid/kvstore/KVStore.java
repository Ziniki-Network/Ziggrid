package org.ziggrid.kvstore;

import org.ziggrid.api.AnalyticItem;
import org.ziggrid.api.StorageEngine;
import org.ziggrid.api.StoreableObject;
import org.zinutils.exceptions.UtilException;

public class KVStore {
	private final StorageEngine storage;
	private final StorageMapper mapper;
	private final KVTransaction tx;
	private final Sender sender;

	public KVStore(StorageEngine storage, StorageMapper mapper, KVTransaction tx) {
		this.storage = storage;
		this.mapper = mapper;
		this.tx = tx;
		this.sender = tx.getSender();
	}
	
	public StorageEngine getStorage() {
		return storage;
	}
	
	public void store(AnalyticItem item) {
		// MAP-ENQUEUE-WRITE
		
		if (tx.contains(item.id()))
			throw new UtilException("Cannot store analytic item with an existing id");
		mapper.mapEnqueue(sender, tx, null, item);
		tx.put(item.id(), item);
	}

	public void write(StoreableObject obj) {
		// READ-MAP-ENQUEUE-WRITE
		StoreableObject prev = null;
		if (tx.contains(obj.id()))
			prev = tx.get(obj.id());
		mapper.mapEnqueue(sender, tx, prev, obj);
		tx.put(obj.id(), obj);
	}

	public StoreableObject get(String key) {
		return tx.get(key);
	}
}
