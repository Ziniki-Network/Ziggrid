package org.ziggrid.utils.jsgen;


public abstract class AbstractForStmt extends Stmt {
	protected final JSVar takes;
	private final JSBlock block;

	AbstractForStmt(JSScope scope, String var) {
		this.takes = scope.getVarLike(var);
		this.block = new JSBlock(scope, this);
	}

	public JSVar getLoopVar() {
		return takes;
	}

	public JSBlock nestedBlock() {
		return block;
	}

	@Override
	public final void toScript(JSBuilder sb) {
		constructFor(sb);
		block.toScript(sb);
	}

	protected abstract void constructFor(JSBuilder sb);
}
