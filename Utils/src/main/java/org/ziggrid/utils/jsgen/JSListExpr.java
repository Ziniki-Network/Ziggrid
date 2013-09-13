package org.ziggrid.utils.jsgen;

import java.util.ArrayList;
import java.util.List;

public class JSListExpr extends JSExpr {
	private final List<JSExpr> members = new ArrayList<JSExpr>();
//	private final JSScope scope;
	
	public JSListExpr(JSScope scope) {
//		this.scope = scope;
	}

	public void add(JSExpr expr) {
		members.add(expr);
	}

	@Override
	public void toScript(JSBuilder sb) {
		sb.osb();
		String sep = "";
		for (JSExpr e : members)
		{
			sb.append(sep);
			e.toScript(sb);
			sep = ",";
		}
		sb.csb();
	}
}
