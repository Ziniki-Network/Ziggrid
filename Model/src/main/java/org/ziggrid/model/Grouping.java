package org.ziggrid.model;

import java.util.ArrayList;
import java.util.List;

import org.ziggrid.utils.utils.PrettyPrinter;
import org.ziggrid.utils.utils.StringUtil;

public class Grouping {
	public final List<String> fields;

	public Grouping(List<String> fields) {
		this.fields = fields != null ? fields : new ArrayList<String>();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void prettyPrint(PrettyPrinter pp) {
		if (fields.isEmpty())
			pp.append("*");
		else
			pp.append(StringUtil.join((List)fields, ", "));
	}

	public String asGroupName() {
		String ret = "";
		if (fields.isEmpty())
			return ret;
		ret += "_groupedBy";
		if (fields.size() > 1) {
			for (int i=0;i<fields.size()-1;i++)
				ret += "_" + fields.get(i);
			ret += "_and";
		}
		ret += "_" + fields.get(fields.size()-1);
		return ret;
	}
}