package org.ziggrid.utils.jsgen;


public class Assign extends Stmt {
	private final String op;
	private final JSExpr to;
	private final JSExpr expr;
	private final boolean declare;

	public Assign(JSVar to, JSExpr expr, boolean declare) {
		this.op = "=";
		this.to = to;
		this.expr = expr;
		this.declare = declare;
	}

	public Assign(ArrayIndex arrayIndex, JSExpr expr) {
		this.op = "=";
		to = arrayIndex;
		this.expr = expr;
		this.declare = false;
	}

	public Assign(LValue member, JSExpr expr) {
		this.op = "=";
		to = member;
		this.expr = expr;
		this.declare = false;
	}

	public Assign(String op, LValue member, JSExpr rvalue) {
		this.op = op;
		to = member;
		expr = rvalue;
		this.declare = false;
	}

	@Override
	public void toScript(JSBuilder sb) {
		if (declare)
			sb.append("var ");
		to.toScript(sb);
		sb.assign(op);
		expr.toScript(sb);
		sb.semi(true);
	}

}
