package org.ziggrid.utils.jsgen;

public class JSThis extends LValue {

	public JSThis(JSScope scope) {
	}

	@Override
	public void toScript(JSBuilder sb) {
		sb.append("this");
	}

}
