package org.ziggrid.utils.jsgen;


public class ForEachStmt extends AbstractForStmt {
	private final JSExpr over;
	private final JSVar idx;

	public ForEachStmt(JSScope scope, String var, JSExpr over) {
		super(scope, var);
		this.over = over;
		idx = scope.getVarLike("idx");
		nestedBlock().add(new Assign(getLoopVar(), new ArrayIndex(over, idx), true));
	}

	@Override
	public void constructFor(JSBuilder sb) {
		sb.ident("for");
		sb.orb();
		sb.ident("var");
		idx.toScript(sb);
		sb.append("=0");
		sb.semi(false);
		idx.toScript(sb);
		sb.append("<");
		over.toScript(sb);
		sb.append(".");
		sb.ident("length");
		sb.semi(false);
		idx.toScript(sb);
		sb.append("++");
		sb.crb();
	}

	public JSExpr getLoopIndex() {
		return idx;
	}
}
