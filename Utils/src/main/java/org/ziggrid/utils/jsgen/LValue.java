package org.ziggrid.utils.jsgen;


public abstract class LValue extends JSExpr {
	
	public JSMethodInvoker method(String method) {
		return new JSMethodInvoker(new JSMember(this, method));
	}

	public JSMember member(String field) {
		return new JSMember(this, field);
	}
}
