package org.ziggrid.utils.jsgen;

public class VarDecl extends JSExprGenerator {
	private final JSVar jsvar;

	VarDecl(JSScope scope, JSVar jsvar) {
		super(scope);
		this.jsvar = jsvar;
	}

	public JSVar getVar() {
		return jsvar;
	}
}
