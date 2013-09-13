package org.ziggrid.driver;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public interface ObserverListener {

	void deliver(Interest i, JSONObject obj) throws JSONException;

}
