package org.ziggrid.utils.jsgen;

public class JSBlockComment extends Stmt {
	private final String text;

	public JSBlockComment(String text) {
		this.text = text;
	}

	@Override
	public void toScript(JSBuilder sb) {
		sb.append("/*" + text + "*/\n");
	}
}
