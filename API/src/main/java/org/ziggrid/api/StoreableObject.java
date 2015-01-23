package org.ziggrid.api;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.zincapi.jsonapi.Payload;
import org.zincapi.jsonapi.PayloadItem;
import org.zinutils.exceptions.UtilException;

public class StoreableObject {
	protected final String id;
	protected final String ziggridType;
	protected final Map<String, Object> fields;

	public StoreableObject(String type, String id) {
		ziggridType = type;
		this.id = id;
		this.fields = new LinkedHashMap<String, Object>();
	}

	public StoreableObject(String type, String id, LinkedHashMap<String, Object> fields) {
		ziggridType = type;
		this.id = id;
		this.fields = fields;
	}

	public String id() {
		return id;
	}

	public void set(String f, Object value) {
		if (f.equals("id") || f.equals("ziggridType"))
			throw new UtilException("Cannot set " + f + " after creation");
		if (!(value instanceof String) && !(value instanceof Boolean) && !(value instanceof Integer) && !(value instanceof Double))
			throw new UtilException("Invalid type for Json to store: " + value.getClass());
		fields.put(f, value);
	}

	public boolean has(String f) {
		return (f.equals("id") || (f.equals("ziggridType") && ziggridType != null) || fields.containsKey(f));
	}
	
	public Object get(String f) {
		if (f.equals("id"))
			return id;
		if (f.equals("ziggridType"))
			return ziggridType;
		if (!fields.containsKey(f))
			throw new UtilException("There is no field " + f + " in " + id + (ziggridType!=null?"@" + ziggridType:"") + ": "+ fields);
		return fields.get(f);
	}

	public boolean is(String test) {
		return ziggridType.equals(test);
	}

	public String asJsonString() throws JSONException {
		return asJsonObject().toString();
	}

	public Payload asPayload() {
		Payload p = new Payload(pluralize(ziggridType));
		PayloadItem item = p.newItem();
		item.set("id", id());
		for (Entry<String, Object> x : fields.entrySet())
			item.set(x.getKey(), x.getValue());
		return p;
	}

	private String pluralize(String name) {
		return name + "s";
	}

	public JSONObject asJsonObject() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("id", id());
		json.put("ziggridType", ziggridType);
		for (Entry<String, Object> x : fields.entrySet())
			json.put(x.getKey(), x.getValue());
		return json;
	}

	public Set<String> keys() {
		Set<String> ret = new HashSet<String>();
		ret.add("id");
		ret.add("ziggridType");
		ret.addAll(fields.keySet());
		return ret;
	}

	@Override
	public String toString() {
		return "{id:"+id+",type:"+ziggridType+",fields:" + fields +"}";
	}
}
