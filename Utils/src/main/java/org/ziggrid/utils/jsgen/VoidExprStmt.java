package org.ziggrid.utils.jsgen;

public class VoidExprStmt extends Stmt {
	private final JSExpr voidExpr;

	public VoidExprStmt(JSExpr voidExpr) {
		this.voidExpr = voidExpr;
	}

	@Override
	public void toScript(JSBuilder sb) {
		voidExpr.toScript(sb);
		if (!(voidExpr instanceof JSFunction))
			sb.semi(true);
	}
}
