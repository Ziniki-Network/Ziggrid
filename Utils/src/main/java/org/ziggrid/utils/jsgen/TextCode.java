package org.ziggrid.utils.jsgen;

public class TextCode extends Stmt {
	private final String code;

	public TextCode(String code) {
		this.code = code;
	}

	@Override
	public void toScript(JSBuilder sb) {
		sb.append(code);
	}
}
