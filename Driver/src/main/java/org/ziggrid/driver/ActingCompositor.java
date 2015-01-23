package org.ziggrid.driver;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.api.StoreableObject;
import org.ziggrid.kvstore.KVStorageEngine;
import org.ziggrid.kvstore.KVStore;
import org.ziggrid.kvstore.KVTransaction;
import org.ziggrid.kvstore.QueuedItem;
import org.ziggrid.model.CompositeDefinition;
import org.ziggrid.model.ObjectDefinition.KeyElement;

public class ActingCompositor extends ActingProcessor {
	protected static final Logger logger = LoggerFactory.getLogger("Snapshot");
	private final String compositeType;
	private final List<KeyElement> keys;

	public ActingCompositor(String sha, KVStorageEngine store, CompositeDefinition cd) {
		super(store, "Compositor");
		addWatchable(cd.into);
		compositeType = cd.into;
		keys = cd.keys;
	}

	@Override
	public StoreableObject canYouProvide(Object inTx, String watchable, Map<String, Object> options) {
		try {
			KVTransaction tx = (KVTransaction) inTx;
			StringBuilder sb = new StringBuilder();
			for (KeyElement k : keys) {
				sb.append(k.extract(options));
			}
			return tx.get(sb.toString());
		} catch (Exception ex) {
			return null;
		}
	}

	@Override
	protected void processMessage(KVTransaction tx, KVStore store, QueuedItem qi) {
		String key = (String)qi.get("_key");
		
		StoreableObject comp;
		if (tx.contains(key)) {
			comp = tx.get(key);
		} else {
			comp = new StoreableObject(compositeType, key);
		}
		
		// Update the "individual" object that we will group later
		for (String f : qi.keys()) {
			if (f.startsWith("_") || f.equals("id") || f.equals("ziggridType"))
				continue;
			comp.set(f, qi.get(f));
		}
		store.write(comp);
	}
}
