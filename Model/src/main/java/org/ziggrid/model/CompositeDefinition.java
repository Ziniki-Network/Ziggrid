package org.ziggrid.model;

import java.util.ArrayList;
import java.util.List;

import org.ziggrid.model.ObjectDefinition.KeyElement;
import org.ziggrid.model.ObjectDefinition.KeyField;
import org.ziggrid.model.ObjectDefinition.KeyString;
import org.ziggrid.utils.utils.PrettyPrinter;

public class CompositeDefinition implements Definition {
	public class ValueField {
		public final String key;
		public final Enhancement value;

		public ValueField(String f, Enhancement v) {
			this.key = f;
			this.value = v;
		}

	}

	public final String into;
	public final String from;
	public final List<KeyElement> keys = new ArrayList<KeyElement>();
	public final List<ValueField> fields = new ArrayList<ValueField>();

	public CompositeDefinition(String into, String from) {
		this.into = into;
		this.from = from;
	}
	
	@Override
	public String context() {
		return "composing " + from + " into " + into;
	}

	public void addKeyString(String ki) {
		keys.add(new KeyString(ki));
	}

	public void addKeyField(String ki) {
		keys.add(new KeyField(ki));
	}

	public void addField(String f, Enhancement v) {
		fields.add(new ValueField(f, v));
	}

	public String getViewName(Grouping grouping) {
		return "compose_"+into + grouping.asGroupName();
	}

	@Override
	public void prettyPrint(PrettyPrinter pp) {
		pp.append("compose ");
		pp.append(from);
		pp.append(" into ");
		pp.append(into);
		pp.append(" {");
		pp.indentMore();
		pp.append("key: ");
		for (KeyElement k : keys) {
			k.prettyPrint(pp);
		}
		pp.append(";");
		pp.requireNewline();
		for (ValueField s : fields) {
			pp.append(s.key + ":");
			s.value.prettyPrint(pp);
			pp.append(";");
			pp.requireNewline();
		}
		pp.indentLess();
		pp.append("}");
		pp.requireNewline();
	}

	@Override
	public String toString() {
		return "compose " + from + " into " + into;
	}
}
