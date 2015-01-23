package org.ziggrid.generator.out.json;

import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.api.AnalyticItem;
import org.ziggrid.api.ExistingObjectProvider;
import org.ziggrid.api.IInterestEngine;
import org.ziggrid.api.IModel;
import org.ziggrid.api.StorageEngine;
import org.ziggrid.api.StoreableObject;
import org.ziggrid.api.TickUpdate;
import org.ziggrid.config.StorageConfig;
import org.zinutils.collections.ListMap;

public class ConsoleDisplay implements StorageEngine {
	public static Logger logger = LoggerFactory.getLogger("ZigGenerator");

	@Override
	public void open(IInterestEngine engine, IModel model, StorageConfig storage) {
	}

	@Override
	public short unique() {
		return 0;
	}

	@Override
	public StoreableObject findExisting(ListMap<String, ? extends ExistingObjectProvider> processors, String tlc, Map<String, Object> options) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean push(TickUpdate toSave) {
		for (AnalyticItem ai : toSave.items) {
			try {
				System.out.println(ai.asJsonString());
			} catch (JSONException ex) {
				logger.error(ex.getMessage());
			}
		}
		return true;
	}

	@Override
	public void close() {
	}

	@Override
	public void recordServer(String string, JSONObject obj) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void syncTo(int id, int currentPosition) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean has(String gameId) {
		// TODO Auto-generated method stub
		return false;
	}
}
