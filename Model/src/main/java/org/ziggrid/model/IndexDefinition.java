package org.ziggrid.model;

import java.util.ArrayList;
import java.util.List;

import org.ziggrid.parsing.ErrorHandler;
import org.ziggrid.utils.collections.CollectionUtils;
import org.ziggrid.utils.utils.PrettyPrinter;

public class IndexDefinition implements Definition {
	private final String viewName;
	public final String ofType;
	private final List<Grouping> groups = new ArrayList<Grouping>();
	public final List<String> values = new ArrayList<String>();

	public IndexDefinition(String viewName, String ofType) {
		this.viewName = viewName;
		this.ofType = ofType;
	}
	
	@Override
	public String context() {
		return "indexing " + ofType + " as " + viewName;
	}

	public void groupBy(List<String> fields) {
		// TODO: we should consider duplicates as that is wasteful
		groups.add(new Grouping(fields));
	}

	public void returnValue(String value) {
		values.add(value);
	}

	public List<Grouping> groupings() {
		if (groups.isEmpty())
			return CollectionUtils.listOf(new Grouping(null));
		else
			return groups;
	}

	public String getViewName(Grouping grouping) {
		return "index_"+viewName + grouping.asGroupName();
	}

	public void complete(ErrorHandler eh, Model model) {
		ObjectDefinition od = model.getModel(eh, ofType);
		for (Grouping grp : groups)
			for (String s : grp.fields)
				if (od.getField(s) == null)
					eh.report(this, "no field " + s + " in " + ofType);
		for (String x : values)
			if (od.getField(x) == null)
				eh.report(this, "no field " + x + " in " + ofType);
	}

	@Override
	public void prettyPrint(PrettyPrinter pp) {
		pp.append("index ");
		pp.append(ofType);
		pp.append(" as ");
		pp.append(viewName);
		pp.append(" {");
		pp.indentMore();
		for (Grouping g : groups) {
			pp.append("provide grouping by ");
			g.prettyPrint(pp);
			pp.append(";");
			pp.requireNewline();
		}
		for (String s : values) {
			pp.append("return ");
			pp.append(s);
			pp.append(";");
			pp.requireNewline();
		}
		pp.requireNewline();
		pp.indentLess();
		pp.append("}");
		pp.requireNewline();
	}

	@Override
	public String toString() {
		return "index " + ofType + " as " + viewName;
	}
}
