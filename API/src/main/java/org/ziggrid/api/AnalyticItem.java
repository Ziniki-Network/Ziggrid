package org.ziggrid.api;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class AnalyticItem extends StoreableObject {
	private final Timestamp at;

	public AnalyticItem(String type, String id, Timestamp at) {
		super(type, type+"_"+id);
		this.at = at;
	}

	@Override
	public JSONObject asJsonObject() throws JSONException {
		JSONObject json = super.asJsonObject();
		json.put("occurred", at.toString());
		return super.asJsonObject();
	}
	@Override
	public String toString() {
		return at.toString() + " [" + id + "]: ";
	}
}
