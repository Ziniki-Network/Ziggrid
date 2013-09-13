package org.ziggrid.utils.jsgen;

import org.ziggrid.utils.exceptions.UtilException;

public class IfElseStmt extends Stmt {
	public JSExpr test;
	public final JSBlock yes;
	public final JSBlock no;
	private final JSScope scope;
	
	public abstract class YesNo extends JSCompiler {
		public YesNo() {
			super(yes);
		}
		
		public void compile() {
			yes();
			changeBlock(no);
			no();
		}
		
		public abstract void yes();
		public abstract void no();
	}
	
	IfElseStmt(JSScope scope) {
		this.scope = scope;
		yes = new JSBlock(scope, this);
		no = new JSBlock(scope, this);
	}

	public IfElseStmt and(JSExpr left, JSExpr right) {
		if (test != null)
			throw new UtilException("Cannot specify more than one test for an if block");
		test = new BinaryOp("&&", left, right);
		return this;
	}

	public IfElseStmt or(JSExpr... exprs) {
		JSExpr fold = null;
		for (JSExpr e : exprs)
			if (fold == null)
				fold = e;
			else
				fold = new BinaryOp("||", fold, e);
		test = fold;
		return this;
	}

	public BinaryOp binop(String op) {
		BinaryOp ret = new BinaryOp(scope, op);
		test = ret;
		return ret;
	}

	public BinaryOp equality() {
		return binop("==");
	}

	public IfElseStmt equality(JSExpr left, JSExpr right) {
		if (test != null)
			throw new UtilException("Cannot specify more than one test for an if block");
		test = new BinaryOp("==", left, right);
		return this;
	}

	public IfElseStmt inequality(JSExpr left, JSExpr right) {
		if (test != null)
			throw new UtilException("Cannot specify more than one test for an if block");
		test = new BinaryOp("!=", left, right);
		return this;
	}

	public IfElseStmt isFalsy(JSExpr expr) {
		if (test != null)
			throw new UtilException("Cannot specify more than one test for an if block");
		test = new UnaryOp("!", expr);
		return this;
	}

	public IfElseStmt isTruthy(JSExpr expr) {
		if (test != null)
			throw new UtilException("Cannot specify more than one test for an if block");
		test = expr;
		return this;
	}

	@Override
	public void toScript(JSBuilder sb) {
		sb.append("if (");
		if (test == null)
			throw new UtilException("test cannot be null in an if statement");
		test.toScript(sb);
		sb.append(")");
		yes.toScript(sb, no.isEmpty());
		if (!no.isEmpty()) {
			sb.append(" else ");
			no.toScript(sb);
		}
	}
}
