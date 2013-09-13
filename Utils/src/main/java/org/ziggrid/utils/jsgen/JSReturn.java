package org.ziggrid.utils.jsgen;

public class JSReturn extends Stmt {
	private JSExpr expr;

	public JSReturn() {
		expr = null;
	}
	
	public JSReturn(JSExpr expr) {
		this.expr = expr;
	}

	@Override
	public void toScript(JSBuilder sb) {
		sb.append("return");
		if (expr != null)
			expr.toScript(sb);
		sb.semi(true);
	}

}
