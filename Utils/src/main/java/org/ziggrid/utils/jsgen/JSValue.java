package org.ziggrid.utils.jsgen;

public class JSValue extends JSExpr {
	private final Object value;

	public JSValue(Object value) {
		this.value = value;
	}
	
	public Object getValue() {
		return value;
	}

	@Override
	public void toScript(JSBuilder sb) {
		sb.writeJSON(value);
	}
}
