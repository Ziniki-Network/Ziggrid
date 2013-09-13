package org.ziggrid.utils.jsgen;

public class NullExpr extends JSExpr {

	@Override
	public void toScript(JSBuilder sb) {
		sb.append("null");
	}
	
}
