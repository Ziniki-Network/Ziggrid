package org.ziggrid.utils.csv;

import java.util.HashMap;

public class CSVLine {

	private HashMap<Integer,String> map = new HashMap<Integer, String>();
	private int count;

	public void put(int pos, String str) {
		if (pos >= count)
			count = pos+1;
		map.put(pos, str);
	}

	public String get(int i) {
		return map.get(i);
	}

	public int getCount()
	{
		return count;
	}
	
	public String toString() {
		StringBuilder ret = new StringBuilder();
		for (int i=0;i<count;i++) {
			ret.append(" ");
			ret.append(map.get(i));
		}
		return ret.substring(1);
	}
}
