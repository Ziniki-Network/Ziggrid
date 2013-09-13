package org.ziggrid.utils.jsgen;


public class ForPropsStmt extends AbstractForStmt {
	private final JSExpr over;

	public ForPropsStmt(JSScope scope, String takes, LValue fields) {
		super(scope, takes);
		this.over = fields;
		MethodCall checkOwnProp = new MethodCall(scope, fields.method("hasOwnProperty"));
		checkOwnProp.arg(getLoopVar());
		IfElseStmt checkOwn = new IfElseStmt(scope).isFalsy(checkOwnProp);
		checkOwn.yes.continueLoop();
		nestedBlock().add(checkOwn);
	}

	@Override
	public void constructFor(JSBuilder sb) {
		sb.ident("for");
		sb.orb();
		sb.ident("var");
		getLoopVar().toScript(sb);
		sb.ident("in");
		over.toScript(sb);
		sb.crb();
	}

}
