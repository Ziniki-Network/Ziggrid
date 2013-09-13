package org.ziggrid.utils.jsgen;

public class JSMember extends LValue {
	private final LValue inside;
	private final String member;

	JSMember(LValue inside, String member) {
		this.inside = inside;
		this.member = member;
	}
	
	JSExpr getOwner() {
		return inside;
	}
	
	String getChild() {
		return member;
	}

	@Override
	public void toScript(JSBuilder sb) {
		inside.toScript(sb);
		sb.append(".");
		sb.append(member);
	}
}
