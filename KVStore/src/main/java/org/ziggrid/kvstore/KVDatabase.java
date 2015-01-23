package org.ziggrid.kvstore;

import com.foundationdb.async.Function;

public interface KVDatabase {

	<T> T run(Function<KVTransaction, T> function);

	KVTransaction beginTx();

	KVQueue queueFrom(int fromQ, int toQ, int range);

}
