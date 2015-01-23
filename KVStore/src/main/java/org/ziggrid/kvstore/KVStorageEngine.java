package org.ziggrid.kvstore;

import org.ziggrid.api.StorageEngine;

public interface KVStorageEngine extends StorageEngine {
	KVStore getStoreForTx(KVTransaction tr);

	KVDatabase database();
}
