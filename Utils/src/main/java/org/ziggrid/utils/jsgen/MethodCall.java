package org.ziggrid.utils.jsgen;

import org.ziggrid.utils.exceptions.UtilException;

public class MethodCall extends FunctionCall {
	private class Dechain extends JSExpr {
		@Override
		public void toScript(JSBuilder sb) {
			handleMe(sb);
		}
	}

	private final JSExpr expr;
	private MethodCall chain;

	MethodCall(JSScope scope, JSExpr expr, String fn) {
		super(scope, fn);
		this.expr = expr;
	}

	MethodCall(JSScope scope, JSMethodInvoker method) {
		super(scope, method.getName());
		this.expr = method.getTarget();
	}

	public MethodCall methodCall(String name, JSExpr... args) {
		if (chain != null)
			throw new UtilException("Illegal or re-chain?");
		chain = new MethodCall(scope, new Dechain(), name);
		for (JSExpr a : args)
			chain.arg(a);
		return chain;
	}

	@Override
	public void toScript(JSBuilder sb) {
		if (chain != null)
			chain.toScript(sb);
		else {
			handleMe(sb);
		}
	}

	private void handleMe(JSBuilder sb) {
		expr.toScript(sb);
		sb.append(".");
		super.toScript(sb);
	}
}
