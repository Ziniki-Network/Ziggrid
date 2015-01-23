package org.ziggrid.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ziggrid.api.Definition;
import org.zinutils.utils.PrettyPrinter;

public class EnhancementDefinition implements Definition {
	public final String from;
	public final String to;
	public final Map<String, Enhancement> fields = new LinkedHashMap<String, Enhancement>();
	public final List<String> fieldNames = new ArrayList<String>();

	public EnhancementDefinition(String from, String enhanced) {
		this.from = from;
		this.to = enhanced;
	}

	@Override
	public String context() {
		return "enhancing from " + from + " to " + to;
	}

	public void define(String ef, Enhancement enhancement) {
		fields.put(ef, enhancement);
		fieldNames.add(ef);
	}
	
	public String getViewName() {
		String name = "enhance_" + to + "_from_" + from;
		return name;
	}
	
	public String keyName(String key) {
		return to + "_from_" + key;
	}

	@Override
	public void prettyPrint(PrettyPrinter pp) {
		pp.append("enhance " + from + " as " + to + " {");
		pp.indentMore();
		for (Entry<String, Enhancement> s : fields.entrySet()) {
			pp.append(s.getKey());
			pp.append(": ");
			s.getValue().prettyPrint(pp);
			pp.requireNewline();
		}
		pp.indentLess();
		pp.append("}");
		pp.requireNewline();
	}
	
	@Override
	public String toString() {
		return "enhance " + from + " as " + to;
	}
}
