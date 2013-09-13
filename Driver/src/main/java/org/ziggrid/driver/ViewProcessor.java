package org.ziggrid.driver;

import java.util.Set;

import org.codehaus.jettison.json.JSONObject;

public interface ViewProcessor extends Runnable {
	Object keyFor(String key, JSONObject obj);
	
	void spill(Set<Object> keys);

	void bump();

	public abstract String toThreadName();
}
