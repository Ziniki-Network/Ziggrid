package org.ziggrid.utils.jsgen;

public class JSNamespace extends LValue {
	private final String s;

	public JSNamespace(String s) {
		this.s = s;
	}

	public JSMember clazz(String member) {
		return new JSMember(this, member);
	}

	@Override
	public void toScript(JSBuilder sb) {
		sb.ident(s);
	}

}
