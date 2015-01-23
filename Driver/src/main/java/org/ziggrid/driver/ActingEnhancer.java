package org.ziggrid.driver;

import java.util.Map;
import java.util.Map.Entry;

import org.ziggrid.api.StoreableObject;
import org.ziggrid.kvstore.KVStorageEngine;
import org.ziggrid.kvstore.KVStore;
import org.ziggrid.kvstore.KVTransaction;
import org.ziggrid.kvstore.QueuedItem;
import org.ziggrid.model.EnhancementDefinition;
import org.zinutils.exceptions.NotImplementedException;

/** An ActingQueue which handles simple enhancement of records.
 * 
 * All the hard work has been done on the writing/mapping end.
 *
 * <p>
 * &copy; 2014 Gareth Powell.  All rights reserved.
 *
 * @author Gareth Powell
 *
 */
public class ActingEnhancer extends ActingProcessor {
	private final String ziggridType;

	public ActingEnhancer(String sha, KVStorageEngine store, EnhancementDefinition d) {
		super(store, "Enhancer");
		this.ziggridType = d.to;
		super.addWatchable(d.to);
	}

	@Override
	public StoreableObject canYouProvide(Object inTx, String watchable, Map<String, Object> options) {
		throw new NotImplementedException("This should be overridden: " + watchable + " in " + this.getClass());
	}

	@Override
	protected void processMessage(KVTransaction tx, KVStore store, QueuedItem qi) {
		String newId = (String) qi.fields.get("_futureId");
		StoreableObject ret = new StoreableObject(ziggridType, newId);
		for (Entry<String, Object> f : qi.fields.entrySet())
			if (!f.getKey().equals("_futureId"))
				ret.set(f.getKey(), f.getValue());
		store.write(ret);
	}
	
	@Override
	public String toString() {
		return "ActingEnhancer[" + watchables() + "]";
	}
}
