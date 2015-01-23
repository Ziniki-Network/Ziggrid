package org.ziggrid.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.ziggrid.api.Definition;
import org.ziggrid.parsing.ErrorHandler;
import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.PrettyPrinter;

public class ObjectDefinition implements Definition {
	public static interface KeyElement {

		void prettyPrint(PrettyPrinter pp);

		Object extract(JSONObject obj) throws JSONException;

		Object extract(Map<String, Object> options);

	}

	public static class KeyString implements KeyElement {
		private final String ki;

		public KeyString(String ki) {
			this.ki = ki;
		}
		
		@Override
		public Object extract(JSONObject obj) {
			return ki;
		}

		@Override
		public Object extract(Map<String, Object> options) {
			return ki;
		}

		@Override
		public void prettyPrint(PrettyPrinter pp) {
			pp.append("\"" + ki + "\"");
		}

	}

	public static class KeyField implements KeyElement {
		final String field;

		public KeyField(String field) {
			this.field = field;
		}

		@Override
		public void prettyPrint(PrettyPrinter pp) {
			pp.append(field);
			
		}

		@Override
		public Object extract(JSONObject obj) throws JSONException {
			return obj.get(field);
		}

		@Override
		public Object extract(Map<String, Object> options) {
			if (!options.containsKey(field))
				throw new UtilException("Cannot recover field " + field);
			return options.get(field);
		}
		
	}

	final String docId;
	public final String name;
	public final List<KeyElement> keys = new ArrayList<KeyElement>();
	private final List<FieldDefinition> fields = new ArrayList<FieldDefinition>();
	private Definition needsCompletion;

	public ObjectDefinition(String docId, String name) {
		this.docId = docId;
		this.name = name;
	}
	
	public void addKeyString(String ki) {
		keys.add(new KeyString(ki));
	}

	public void addKeyField(String ki) {
		keys.add(new KeyField(ki));
	}

	public String computeKey(Map<String, Object> options) {
		StringBuilder sb = new StringBuilder();
		for (KeyElement ke : keys) {
			if (ke instanceof KeyString)
				sb.append(((KeyString) ke).ki);
			else if (ke instanceof KeyField)
				sb.append(options.get(((KeyField) ke).field));
		}
		return sb.toString();
	}

	public void setCompleter(Definition d) {
		needsCompletion = d;
	}
	
	@Override
	public String context() {
		return "defining object " + name;
	}

	public void complete(ErrorHandler eh, Model model) {
		if (needsCompletion != null)
			model.validate(eh, needsCompletion);
		/*
		List<FieldDefinition> key = new ArrayList<FieldDefinition>();
		List<FieldDefinition> value = new ArrayList<FieldDefinition>();
		for (FieldDefinition f: fields)
			if (f.isKey)
				key.add(f);
			else
				value.add(f);
		while (key.size() > 0) {
			key.remove(key.size()-1);
			ObjectDefinition keyed = new ObjectDefinition(docId, name + "-key" + key.size());
			for (FieldDefinition f : key)
				keyed.addField(f);
			for (FieldDefinition f : value)
				keyed.addField(f);
			model.add(eh, keyed.docId, keyed);
		}
		*/
	}

	public void addField(FieldDefinition field) {
		fields.add(field);
	}
	
	public FieldDefinition getField(String key) {
		for (FieldDefinition f : fields) {
			if (f.name.equals(key))
				return f;
		}
		return null;
	}

	public List<FieldDefinition> fields() {
		return fields;
	}

	@Override
	public void prettyPrint(PrettyPrinter pp) {
		pp.append("object " + name + " {");
		pp.indentMore();
		for (FieldDefinition fd : fields) {
			fd.prettyPrint(pp);
		}
		pp.indentLess();
		pp.append("}");
		pp.requireNewline();
	}

	@Override
	public String toString() {
		return "object " + name;
	}

}
