package org.ziggrid.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TickUpdate {
	public Map<String, StoreableObject> updates = new HashMap<String, StoreableObject>();
	public List<AnalyticItem> items = new ArrayList<AnalyticItem>();
}
