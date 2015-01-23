package org.ziggrid.kvstore;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class QueuedItem {
	public final String qid;
	public final String procSHA;
	public final Map<String,Object> fields = new HashMap<String,Object>();

	public QueuedItem(String qid, String procSHA) {
		this.qid = qid;
		this.procSHA = procSHA;
	}
	
	public Set<String> keys() {
		return fields.keySet();
	}
	
	public Object get(String f) {
		return fields.get(f);
	}

	@Override
	public String toString() {
		return procSHA + " => " + fields;
	}
}
