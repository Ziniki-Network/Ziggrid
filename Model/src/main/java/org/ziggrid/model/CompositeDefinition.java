package org.ziggrid.model;

import java.util.ArrayList;
import java.util.List;

import org.ziggrid.api.Definition;
import org.ziggrid.model.ObjectDefinition.KeyElement;
import org.ziggrid.model.ObjectDefinition.KeyField;
import org.ziggrid.model.ObjectDefinition.KeyString;
import org.zinutils.utils.PrettyPrinter;

public class CompositeDefinition implements Definition {
	public final String into;
	public final String from;
	public final List<KeyElement> keys = new ArrayList<KeyElement>();
	public final List<NamedEnhancement> fields = new ArrayList<NamedEnhancement>();

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

	public void addField(NamedEnhancement v) {
		fields.add(v);
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
		for (NamedEnhancement s : fields) {
			pp.append(s.name + ":");
			s.enh.prettyPrint(pp);
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
