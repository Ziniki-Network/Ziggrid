package org.ziggrid.driver;

import org.codehaus.jettison.json.JSONArray;

public class LeaderboardSummary {
	public final JSONArray fieldsValues;
	public final JSONArray storeAs;

	public LeaderboardSummary(JSONArray fieldsValues, JSONArray storeAs) {
		this.fieldsValues = fieldsValues;
		this.storeAs = storeAs;
	}
}
