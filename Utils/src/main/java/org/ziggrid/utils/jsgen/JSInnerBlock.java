package org.ziggrid.utils.jsgen;

public class JSInnerBlock extends Stmt {
	private JSBlock block;

	public JSInnerBlock(JSScope scope) {
		block = new JSBlock(scope, this, false);
	}

	@Override
	public void toScript(JSBuilder sb) {
		block.toScript(sb);
	}

	public JSBlock getBlock() {
		return block;
	}
}
