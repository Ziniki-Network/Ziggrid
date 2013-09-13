package org.ziggrid.utils.jsgen;

public class ArrayIndex extends LValue {
	private final JSExpr array;
	private final JSExpr idx;

	ArrayIndex(JSExpr array, JSExpr idx) {
		this.array = array;
		this.idx = idx;
	}

	@Override
	public void toScript(JSBuilder sb) {
		array.toScript(sb);
		sb.osb();
		idx.toScript(sb);
		sb.csb();
	}

}
