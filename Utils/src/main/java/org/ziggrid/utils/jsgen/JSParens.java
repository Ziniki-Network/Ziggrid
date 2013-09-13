package org.ziggrid.utils.jsgen;

public class JSParens extends JSExpr {
	private JSExpr expr;

	public JSParens(JSExpr expr) {
		this.expr = expr;
	}

	@Override
	public void toScript(JSBuilder sb) {
		sb.append("(");
		expr.toScript(sb);
		sb.append(")");
	}

}
