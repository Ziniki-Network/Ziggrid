package org.ziggrid.driver;

import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.ziggrid.exceptions.ZiggridException;
import org.ziggrid.model.FieldDefinition;
import org.ziggrid.model.ObjectDefinition;

public class Interest {
	private final ObserverSender sender;
	public final int handle;
	public final String table;
	private final Map<String, Object> options;
	final String sha;
	public final boolean haveFullKey;

	public Interest(MaterializeObjects materializer, ObserverSender sender, int handle, String table, ObjectDefinition defn, Map<String, Object> options) {
		JSONArray key = new JSONArray();
		boolean fullKey = true;
		for (FieldDefinition f : defn.fields())
			if (f.isKey) {
				if (!options.containsKey(f.name)) {
					fullKey = false;
					continue; // throw new ZiggridException("The key field " + f.name + " was not specified when trying to watch " + table);
				}
				key.put(options.get(f.name));
			}
		if (options.size() > key.length())
			throw new ZiggridException("There were too many options provided to watch " + table);
		this.haveFullKey = fullKey;
		this.sender = sender;
		this.handle = handle;
		this.table = table.toLowerCase();
		this.options = options;
		if (defn.keys.isEmpty())
			this.sha = materializer.computeSHAId(defn.name, key.toString());
		else
			this.sha = defn.computeKey(options);
	}

	public boolean appliesTo(JSONObject obj) {
		for (Entry<String, Object> x : options.entrySet()) {
			try {
				Object tmp = obj.get(x.getKey());
				if (tmp == null)
					return false;
				if (!tmp.equals(x.getValue()))
					return false;
			} catch (JSONException ex) {
				return false;
			}
		}
		return true;
	}

	public void deliver(JSONObject payload) {
		sender.deliver(this, payload);
	}
	
	@Override
	public String toString() {
		return "Interest("+handle+") in " + table + " with " + options;
	}
}
