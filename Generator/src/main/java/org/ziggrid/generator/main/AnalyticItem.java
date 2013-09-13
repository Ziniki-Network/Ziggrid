package org.ziggrid.generator.main;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.ziggrid.generator.main.Timer.Timestamp;
import org.ziggrid.generator.provider.Factory;
import org.ziggrid.utils.exceptions.UtilException;

public class AnalyticItem {
	private final String ziggridType;
	private final int id;
	private final Timestamp at;
	private final Map<String, Object> fields = new LinkedHashMap<String, Object>();

	public AnalyticItem(Factory factory, Timer timer) {
		this.id = factory.nextId();
		this.at = timer.notch();
		this.ziggridType = this.getClass().getSimpleName();
	}

	public AnalyticItem(Factory factory, Timer timer, String type) {
		this.id = factory.nextId();
		this.at = timer.notch();
		this.ziggridType = type;
	}

	public String id() {
		return ziggridType+"_"+id;
	}

	public void set(String string, Object value) {
		if (string.equals("ziggridType"))
			throw new UtilException("Cannot set ziggridType after creation");
		if (fields.containsKey(string))
			throw new UtilException("Duplicate field " + string);
		if (!(value instanceof String) && !(value instanceof Boolean) && !(value instanceof Integer))
			throw new UtilException("Invalid type for Json to store: " + value.getClass());
		fields.put(string, value);
	}

	public Object get(String string) {
		if (!fields.containsKey(string))
			throw new UtilException("There is no field " + string);
		return fields.get(string);
	}

	@Override
	public String toString() {
		return at.toString() + " [" + id + "]: ";
	}

	public String asJson() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("id", id());
		json.put("ziggridType", ziggridType);
		json.put("occurred", at.toString());
		for (Entry<String, Object> x : fields.entrySet())
			json.put(x.getKey(), x.getValue());
		return json.toString();
	}

	public boolean is(String string) {
		return ziggridType.equals(string);
	}
}
