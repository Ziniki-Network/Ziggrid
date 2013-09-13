package org.ziggrid.utils.jsgen;

import java.util.ArrayList;
import java.util.List;

public class FunctionCall extends JSExpr {
	private final String fn;
	private final List<JSExpr> args = new ArrayList<JSExpr>();
	protected final JSScope scope;
	private final JSExpr expr;

	FunctionCall(JSScope scope, String fn) {
		this.scope = scope;
		this.fn = fn;
		this.expr = null;
	}

	public FunctionCall(JSScope scope, JSExpr expr) {
		this.scope = scope;
		this.fn = null;
		this.expr = expr;
	}

	// If you already have the argument ...
	public void arg(JSExpr a) {
		args.add(a);
	}

	// If you want to create something just for here
	public JSExprGenerator arg() {
		JSExprGenerator ret = new JSExprGenerator(scope);
		args.add(ret);
		return ret;
	}
	
	public FunctionCall arg(int k) {
		arg().value(k);
		return this;
	}

	public FunctionCall arg(String s) {
		arg().value(s);
		return this;
	}

	@Override
	public void toScript(JSBuilder sb) {
		if (fn != null)
			sb.append(fn);
		else
			expr.toScript(sb);
		sb.append("(");
		String sep = "";
		for (JSExpr ce : args)
		{
			sb.append(sep);
			if (ce == null)
				sb.append("null");
			else
				ce.toScript(sb);
			sep = ",";
		}
		sb.append(")");
	}

}
