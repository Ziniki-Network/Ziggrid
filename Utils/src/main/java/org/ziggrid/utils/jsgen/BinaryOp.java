package org.ziggrid.utils.jsgen;

import org.ziggrid.utils.exceptions.UtilException;

public class BinaryOp extends JSExpr {

	private final String op;
	private JSExpr left;
	private JSExpr right;
	private final JSScope scope;

	public BinaryOp(JSScope scope, String op) {
		this.scope = scope;
		this.op = op;
	}

	public BinaryOp(String op, JSExpr left, JSExpr right) {
		scope = null;
		this.op = op;
		this.left = left;
		this.right = right;
	}

	public JSExprGenerator arg() {
		JSExprGenerator ret = new JSExprGenerator(scope);
		if (left == null) {
			left = ret;
		} else if (right == null) {
			right = ret;
		} else
			throw new UtilException("Cannot specify more than 2 operands to a binop");
		return ret;
	}

	public BinaryOp arg(JSExpr jsValue) {
		arg().value(jsValue);
		return this;
	}

	public BinaryOp arg(int k) {
		arg(new JSValue(k));
		return this;
	}

	@Override
	public void toScript(JSBuilder sb) {
		// TODO: We probably need to consider precedence at some point
		if (left == null)
			sb.append("null");
		else
			left.toScript(sb);
		sb.append(" " + op + " ");
		if (right == null)
			sb.append("null");
		else
			right.toScript(sb);
	}

}
