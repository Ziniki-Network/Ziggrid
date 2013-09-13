package org.ziggrid.utils.jsgen;

import org.ziggrid.utils.exceptions.UtilException;

public class JSExprGenerator extends JSExpr {
	private final JSScope scope;
	private JSExpr expr;

	JSExprGenerator(JSScope scope) {
		this.scope = scope;
	}
	
	@Override
	public void toScript(JSBuilder sb) {
		if (expr == null)
			throw new UtilException("Cannot handle placeholder which was never used");
		expr.toScript(sb);
	}

	public void value(String string) {
		if (expr != null)
			throw new UtilException("Can only specify one value in a placeholder");
		expr = new JSValue(string);
	}
	
	public void value(JSExpr e) {
		if (expr != null)
			throw new UtilException("Can only specify one value in a placeholder");
		if (e == null)
			e = new JSValue(null);
		expr = e;
	}
	
	public void value(int k) {
		if (expr != null)
			throw new UtilException("Can only specify one value in a placeholder");
		expr = new JSValue(k);
	}
	
	
	public MethodCall methodCall(JSExpr obj, String method) {
		if (expr != null)
			throw new UtilException("Can only specify one value in a placeholder");
		expr = new MethodCall(scope, obj, method);
		return (MethodCall) expr;
	}

	public MethodCall methodCall(JSMethodInvoker method) {
		if (expr != null)
			throw new UtilException("Can only specify one value in a placeholder");
		expr = new MethodCall(scope, method);
		return (MethodCall) expr;
	}

	public JSMember member(LValue inside, String m) {
		if (expr != null)
			throw new UtilException("Can only specify one value in a placeholder");
		expr = new JSMember(inside, m);
		return (JSMember) expr;
	}

	public JSObjectExpr literalObject() {
		if (expr != null)
			throw new UtilException("Can only specify one value in a placeholder");
		JSObjectExpr ret = new JSObjectExpr(scope, true);
		expr = ret;
		return ret;
	}
	
	public JSListExpr list() {
		if (expr != null)
			throw new UtilException("Can only specify one value in a placeholder");
		JSListExpr ret = new JSListExpr(scope);
		expr = ret;
		return ret;
	}
	
	public ArrayIndex subscript(JSMember internals, JSValue jsValue) {
		if (expr != null)
			throw new UtilException("Can only specify one value in a placeholder");
		ArrayIndex ret = new ArrayIndex(internals, jsValue);
		expr = ret;
		return ret;
	}
}
