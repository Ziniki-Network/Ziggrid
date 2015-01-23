package org.ziggrid.model;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.api.Definition;
import org.ziggrid.exceptions.ZiggridException;
import org.ziggrid.parsing.ErrorHandler;
import org.zinutils.utils.PrettyPrinter;

public class CorrelationDefinition implements Definition {
	private static final Logger logger = LoggerFactory.getLogger("CorrelationDefinition");
	public final String docId;
	public final String from;
	public final String name;
	private final String globalView;
	public Enhancement value;
	public final List<NamedEnhancement> items = new ArrayList<NamedEnhancement>();
	private final List<Grouping> groups = new ArrayList<Grouping>();

	public CorrelationDefinition(String docId, String name) {
		this.docId = docId;
		this.from = name;
		this.name = "correlate_on_"+name;
		this.globalView = "correlate_global_on_" + name;
	}
	
	@Override
	public String context() {
		return "creating correlation for " + from;
	}

	public void useValue(Enhancement value) {
		this.value = value;
	}

	public void addCaseItem(NamedEnhancement item) {
		items.add(item);
	}

	public void groupBy(ErrorHandler eh, Model m, List<String> g) {
		Grouping ret = new Grouping(g);
		groups.add(ret);
		String oname = getViewName(ret);
		ObjectDefinition od = new ObjectDefinition(docId, oname);
		m.add(eh, docId, od);
		od.setCompleter(this);
	}

	public String getGlobalViewName() {
		return globalView;
	}

	public List<Grouping> groupings() {
		return groups;
	}

	public String getViewName(Grouping grouping) {
		return name + grouping.asGroupName();
	}

	public void complete(ErrorHandler eh, Model model) {
		ObjectDefinition about = model.getModel(eh, from);
		if (about == null)
			throw new ZiggridException("There is no model " + from);
		for (Grouping grp : groups)
		{
			ObjectDefinition u = model.getModel(eh, getViewName(grp));
			int fno = 0;
			for (String s : grp.fields)
				u.addField(new FieldDefinition(s, about.getField(s).type, true));
			for (NamedEnhancement c : this.items) {
				u.addField(c.fieldDefinition(about));
			}
			u.addField(new FieldDefinition("correlation", "number", false));
		}
	}

	@Override
	public void prettyPrint(PrettyPrinter pp) {
		pp.append("correlate on ");
		pp.append(from);
		pp.append(" {");
		pp.indentMore();
		pp.append("correlate value ");
		value.prettyPrint(pp);
		pp.append(";");
		pp.requireNewline();
		for (Grouping g : groups) {
			pp.append("provide grouping by ");
			g.prettyPrint(pp);
			pp.append(";");
			pp.requireNewline();
		}
		pp.append("divide into cases on ");
		String sep = "";
		for (NamedEnhancement s : items) {
			pp.append(sep);
			s.enh.prettyPrint(pp);
			sep = ", ";
		}
		pp.append(";");
		pp.requireNewline();
		/*
		for (String s : values) {
			pp.append("return ");
			pp.append(s);
			pp.append(";");
			pp.requireNewline();
		}
		pp.append("top " + top +";");
		pp.requireNewline();
		 */
		pp.indentLess();
		pp.append("}");
		pp.requireNewline();
	}

	public String toString() {
		return "correlate on " + from;
	};
}
