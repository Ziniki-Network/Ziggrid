package org.ziggrid.model;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.exceptions.ZiggridException;
import org.ziggrid.parsing.ErrorHandler;
import org.ziggrid.utils.collections.CollectionUtils;
import org.ziggrid.utils.utils.PrettyPrinter;

public class LeaderboardDefinition implements Definition {
	private static final Logger logger = LoggerFactory.getLogger("LeaderboardDefinition");
	private final String docId;
	public final String name;
	private final String entryName;
	public final String from;
	private final List<Grouping> groups = new ArrayList<Grouping>();
	public final List<Enhancement> sorts = new ArrayList<Enhancement>();
	public final List<Enhancement> filters = new ArrayList<Enhancement>();
	public final List<String> values = new ArrayList<String>();
	public int top;
	public boolean ascending;

	public LeaderboardDefinition(String docId, String name, String from) {
		this.docId = docId;
		this.name = "leaderboard_"+name;
		this.entryName = "leaderboardEntry_" + name;
		this.from = from;
	}

	@Override
	public String context() {
		return "defining leaderboard " + name + " from " + from;
	}

	public void groupBy(ErrorHandler eh, Model model, List<String> fields) {
		// TODO: we should consider duplicates as that is wasteful
		Grouping ret = new Grouping(fields);
		groups.add(ret);
		ObjectDefinition listDefn = new ObjectDefinition(docId, getViewName(ret));
		listDefn.setCompleter(this);
		FieldDefinition field = new FieldDefinition("table", "list", false);
		String eName = getEntryName(ret);
		field.addParam(eName);
		listDefn.addField(field);
		model.add(eh, docId, listDefn);
		ObjectDefinition entry = new ObjectDefinition(docId, eName);
		entry.setCompleter(this);
		model.add(eh, docId, entry);
	}

	public void sortBy(Enhancement enhancement) {
		sorts.add(enhancement);
	}

	public void filter(Enhancement enhancement) {
		filters.add(enhancement);
	}

	public void returnValue(String value) {
		values.add(value);
	}

	public void top(int top) {
		this.top = top;
	}

	public List<Grouping> groupings() {
		if (groups.isEmpty())
			return CollectionUtils.listOf(new Grouping(null));
		else
			return groups;
	}
	
	public String getViewName(Grouping grouping) {
		return name + grouping.asGroupName();
	}

	public String getEntryName(Grouping grouping) {
		return entryName + grouping.asGroupName();
	}

	public void complete(ErrorHandler eh, Model model) {
		ObjectDefinition about = model.getModel(eh, from);
		if (about == null)
			throw new ZiggridException("There is no model " + from);
		for (Grouping grp : groups)
		{
			ObjectDefinition v = model.getModel(eh, getViewName(grp));
			if (v == null) throw new ZiggridException("Couldn't find my own leaderboard class");
			for (String s : grp.fields)
				v.addField(new FieldDefinition(s, about.getField(s).type, true));
			
			ObjectDefinition u = model.getModel(eh, getEntryName(grp));
			if (u == null) throw new ZiggridException("Couldn't find my own entry class");
			int fno = 0;
			for (Enhancement c : this.sorts) {
				if (c instanceof FieldEnhancement) {
					String fname = ((FieldEnhancement)c).field;
					if (about.getField(fname) == null)
						eh.report(this, "There is no field " + fname + " in " + from);
					else
						u.addField(new FieldDefinition(fname, about.getField(fname).type, false));
				} else {
					logger.error("Don't have a field name for enhancement " + c);
					u.addField(new FieldDefinition("f" + (++fno), "number", false));
				}
			}
			for (String c : this.values)
				if (about.getField(c) == null)
					eh.report(this, "There is no field " + c + " in " + from);
				else
					u.addField(new FieldDefinition(c, about.getField(c).type, false));
		}
	}

	@Override
	public void prettyPrint(PrettyPrinter pp) {
		pp.append("leaderboard ");
		pp.append(name);
		pp.append(" from ");
		pp.append(from);
		pp.append(" {");
		pp.indentMore();
		for (Grouping g : groups) {
			pp.append("provide grouping by ");
			g.prettyPrint(pp);
			pp.append(";");
			pp.requireNewline();
		}
		for (Enhancement s : sorts) {
			pp.append("sort by ");
			s.prettyPrint(pp);
			pp.append(";");
			pp.requireNewline();
		}
		for (String s : values) {
			pp.append("return ");
			pp.append(s);
			pp.append(";");
			pp.requireNewline();
		}
		pp.append("top " + top +";");
		pp.requireNewline();
		pp.indentLess();
		pp.append("}");
		pp.requireNewline();
	}

	@Override
	public String toString() {
		return "Leaderboard for " + name + " from " + from;
	}
}
