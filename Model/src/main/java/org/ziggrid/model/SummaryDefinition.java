package org.ziggrid.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ziggrid.utils.exceptions.UtilException;
import org.ziggrid.utils.utils.PrettyPrinter;

public class SummaryDefinition implements Definition {
	public final String docId;
	public final String summary;
	public final String event;
	public final List<MatchField> matches = new ArrayList<MatchField>();
	public final Map<String, Reduction> reductions = new LinkedHashMap<String, Reduction>();
	private final List<String> allFields = new ArrayList<String>();
	private final Map<Integer, List<String>> matchFields = new HashMap<Integer,List<String>>();
	private final List<String> reduceFields = new ArrayList<String>();
	private Integer which;

	public SummaryDefinition(String docId, String summary, String event) {
		this.docId = docId;
		this.summary = summary;
		this.event = event;
	}
	
	@Override
	public String context() {
		return "summarizing " + summary + " from " + event + whichString();
	}

	public void setWhich(int i) {
		which = i;
	}
	
	public String whichString() {
		if (which == null)
			return "";
		else
			return "_" + which;
	}

	public boolean trailblazer() {
		return which == null || which == 1;
	}

	public void match(String summaryField, String eventField) {
		matches.add(new MatchField(summaryField, eventField));
		allFields.add(summaryField);
		List<String> key;
		if (matchFields.isEmpty())
			key = new ArrayList<String>();
		else
			key = new ArrayList<String>(matchFields.get(matchFields.size()));
		key.add(summaryField);
		matchFields.put(key.size(), key);
	}
	
	public void reduceTo(String reduceTo, Reduction reduction) {
		if (reductions.containsKey(reduceTo))
			throw new UtilException("Duplicate reduce key: " + reduceTo);
		reductions.put(reduceTo, reduction);
		allFields.add(reduceTo);
		reduceFields.add(reduceTo);
	}

	public List<String> summaryFields() {
		return allFields;
	}

	public List<String> keyFields(int wanted) {
		return matchFields.get(wanted);
	}

	public List<String> valueFields() {
		return reduceFields;
	}

	public String getViewName() {
		return "reduce_"+summary+"_from_"+event + whichString();
	}

	@Override
	public void prettyPrint(PrettyPrinter pp) {
		pp.append("reduce ");
		pp.append(summary);
		pp.append(" from ");
		pp.append(event);
		pp.append(" {");
		pp.indentMore();
		for (MatchField m : matches)
			m.prettyPrint(pp);
		for (Entry<String, Reduction> r : reductions.entrySet()) {
			pp.append(r.getKey());
			r.getValue().prettyPrint(pp);
			pp.requireNewline();
		}
		pp.indentLess();
		pp.append("}");
		pp.requireNewline();
	}
	
	@Override
	public String toString() {
		return "reduce " + summary + " from " + event;
	}
}
