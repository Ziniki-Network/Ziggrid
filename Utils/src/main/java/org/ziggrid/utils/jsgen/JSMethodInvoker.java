package org.ziggrid.utils.jsgen;

public class JSMethodInvoker {
	private final JSExpr target;
	private final String name;

	JSMethodInvoker(JSMember member) {
		target = member.getOwner();
		name = member.getChild();
	}

	public String getName() {
		return name;
	}

	public JSExpr getTarget() {
		return target;
	}
}
