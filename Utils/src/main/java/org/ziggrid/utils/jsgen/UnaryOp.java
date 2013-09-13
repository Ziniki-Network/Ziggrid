package org.ziggrid.utils.jsgen;

public class UnaryOp extends JSExpr {

	private final String op;
	private final JSExpr expr;

	public UnaryOp(String op, JSExpr expr) {
		this.op = op;
		this.expr = expr;
	}

	@Override
	public void toScript(JSBuilder sb) {
		sb.append(" " + op + " ");
		if (expr instanceof JSVar)
			expr.toScript(sb);
		else {
			sb.append("(");
			expr.toScript(sb);
			sb.append(")");
		}
	}

}
