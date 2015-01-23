package org.ziggrid.api;

import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.ziggrid.config.StorageConfig;
import org.zinutils.collections.ListMap;

public interface StorageEngine {
	void open(IInterestEngine engine, IModel model, StorageConfig storage);
	short unique();
	boolean has(String gameId);
	void syncTo(int id, int currentPosition);
	boolean push(TickUpdate toSave);
	StoreableObject findExisting(ListMap<String, ? extends ExistingObjectProvider> processors, String tlc, Map<String, Object> options);
	void close();
	void recordServer(String string, JSONObject obj);
}
