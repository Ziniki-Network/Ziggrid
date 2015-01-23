package org.ziggrid.api;

import org.codehaus.jettison.json.JSONArray;

public class TransmittableObject extends StoreableObject {

	public class Table {
		private JSONArray ret;

		public Table(JSONArray ret) {
			this.ret = ret;
		}

		public void add(Object fvalue, Object value) {
			JSONArray inner = new JSONArray();
			ret.put(inner);
			inner.put(fvalue);
			inner.put(value);
		}
	}

	public TransmittableObject(String type, String id) {
		super(type, id);
	}

	public Table table(String string) {
		JSONArray ret = new JSONArray();
		fields.put(string, ret);
		return new Table(ret);
	}

}
