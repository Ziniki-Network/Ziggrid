package org.ziggrid.kvstore.memory;

import java.util.Map;

import org.ziggrid.kvstore.KVTransaction;
import org.ziggrid.kvstore.Sender;

// This should be extracted to derive from a class it can share with FoundationKVStore
// The main purpose of this class is to handle creating queuing objects
public class MemorySender implements Sender {
	private final MemoryDatabase db;

	public MemorySender(MemoryDatabase db) {
		this.db = db;
	}

	@Override
	public void queue(KVTransaction tx, String sha, byte[] itemId, Map<String,Object> fields) {
		MemoryQueue q = db.getQueue(itemId);
		if (q != null)
			q.put(sha, itemId, fields);
	}
}
