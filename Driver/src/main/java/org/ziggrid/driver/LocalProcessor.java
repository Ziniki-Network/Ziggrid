package org.ziggrid.driver;

import org.codehaus.jettison.json.JSONObject;
import org.ziggrid.utils.collections.ListMap;

public interface LocalProcessor extends ViewProcessor {
	void spill(ListMap<Object, JSONObject> keyedEntries);
}
