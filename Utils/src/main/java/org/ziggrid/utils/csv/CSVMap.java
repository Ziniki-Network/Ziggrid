package org.ziggrid.utils.csv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CSVMap {

	private Map<String, String> map = new HashMap<String, String>();
	private final ArrayList<String> headers;

	public CSVMap(ArrayList<String> headers, CSVLine thisLine) {
		this.headers = headers;
		for (int i=0;i<headers.size() && i<thisLine.getCount();i++)
			map.put(headers.get(i), thisLine.get(i));
	}

	public CSVMap(ArrayList<String> headers, ArrayList<String> values) {
		this.headers = headers;
		for (int i=0;i<headers.size() && i<values.size();i++)
			map.put(headers.get(i), values.get(i));
	}

	public String get(String field) {
		if (!map.containsKey(field) || map.get(field) == null)
			return "";
		return map.get(field);
	}

	public List<String> headers() {
		return headers;
	}

}
