package org.ziggrid.driver.interests;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.ziggrid.driver.ObserverListener;
import org.ziggrid.model.ObjectDefinition;

public class Interest {
	public final ObserverListener lsnr;
	public final String table;
	private final Map<String, Object> options;
	private final Set<InterestTable> feeds = new HashSet<InterestTable>();

	public final String sha;
	public final boolean haveFullKey;

	// What we want to move towards
	public Interest(ObserverListener lsnr, String table, ObjectDefinition defn, Map<String, Object> options) {
		this.lsnr = lsnr;
		this.options = options;
		this.table = table.toLowerCase();
		
		
		// I claim these are deprecated
		this.sha = null;
		this.haveFullKey = false;
	}
	
	// Old-school couchbase constructor
	/*
	public Interest(MaterializeObjects materializer, ObserverListener sender, int handle, String table, ObjectDefinition defn, Map<String, Object> options) {
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
		this.lsnr = sender;
//		this.handle = handle;
		this.table = table.toLowerCase();
		this.options = options;
		if (defn.keys.isEmpty())
			this.sha = materializer.computeSHAId(defn.name, key.toString());
		else
			this.sha = defn.computeKey(options);
	}
	*/

	// This is deprecated - use the interest engine instead ...
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
	
	public void feeds(InterestTable table) {
		feeds.add(table);
	}

	public void cleanUp() {
		for (InterestTable table : feeds) {
			table.removeInterest(this);
		}
	}

	@Override
	public String toString() {
		return "Interest("+lsnr+") in " + table + " with " + options;
	}
}
