package org.ziggrid.utils.jsgen;

public class JSName extends JSExpr {
	private final String name;

	public JSName(String name) {
		this.name = name;
	}
	
	@Override
	public void toScript(JSBuilder sb) {
		sb.append(name);
	}
}
