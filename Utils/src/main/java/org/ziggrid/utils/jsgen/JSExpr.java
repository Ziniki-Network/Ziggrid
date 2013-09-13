package org.ziggrid.utils.jsgen;


public abstract class JSExpr implements JSEntry {
	
	public ArrayIndex subscript(String s) {
		return new ArrayIndex(this, new JSValue(s));
	}

	public ArrayIndex subscript(int k) {
		return new ArrayIndex(this, new JSValue(k));
	}

	public ArrayIndex subscript(JSExpr expr) {
		return new ArrayIndex(this, expr);
	}
}
