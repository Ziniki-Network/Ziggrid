package org.ziggrid.generator.out.ws;

import java.util.Map;

import org.atmosphere.cpr.AsyncIOWriter;
import org.atmosphere.cpr.AtmosphereResponse;
import org.codehaus.jettison.json.JSONObject;
import org.ziggrid.api.AnalyticItem;
import org.ziggrid.api.ExistingObjectProvider;
import org.ziggrid.api.IInterestEngine;
import org.ziggrid.api.IModel;
import org.ziggrid.api.StorageEngine;
import org.ziggrid.api.StoreableObject;
import org.ziggrid.api.TickUpdate;
import org.ziggrid.config.StorageConfig;
import org.zinutils.collections.ListMap;

public class WebSocketSender implements StorageEngine {
	private AtmosphereResponse response;
	private AsyncIOWriter writer;

	public WebSocketSender(AtmosphereResponse response) {
		this.response = response;
		this.writer = response.getAsyncIOWriter();
	}

	@Override
	public void open(IInterestEngine engine, IModel model, StorageConfig storage) {
	}

	@Override
	public short unique() {
		return 0;
	}

	@Override
	public StoreableObject findExisting(ListMap<String, ? extends ExistingObjectProvider> processors, String tlc, Map<String, Object> options) {
		return null;
	}

	@Override
	public synchronized boolean push(TickUpdate toSave) {
		if (writer == null)
			return false;
		for (AnalyticItem item : toSave.items) {
			try {
				writer.write(response, item.asJsonString());
			} catch (Exception notAgain) {
				notAgain.printStackTrace();
				writer = null;
				return false;
			}
		}
		return true;
	}

	@Override
	public synchronized void close() {
		writer = null;
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
		return false;
	}
}
