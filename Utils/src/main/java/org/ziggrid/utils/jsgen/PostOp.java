package org.ziggrid.utils.jsgen;

public class PostOp extends JSExpr {
	private final String op;
	private final JSExpr expr;

	public PostOp(String op, JSExpr expr) {
		this.op = op;
		this.expr = expr;
	}

	@Override
	public void toScript(JSBuilder sb) {
		if (expr instanceof JSVar)
			expr.toScript(sb);
		else {
			sb.append("(");
			expr.toScript(sb);
			sb.append(")");
		}
		sb.append(" " + op + " ");
	}

}
