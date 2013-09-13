package org.ziggrid.utils.jsgen;

import java.util.HashMap;
import java.util.Map;

public class JSFile {
	private final JSScope scope = new JSScope(null);
	private final JSBlock block = new JSBlock(scope, null, false);
	private final Map<String, JSNamespace> namespaces = new HashMap<String, JSNamespace>();

	public JSBuilder getBuilder() {
		JSBuilder builder = new JSBuilder();
		builder.setPretty(true);
		return builder;
	}
	
	public JSBlock getBlock() {
		return block;
	}

	public JSScope getScope() {
		return scope;
	}
	
	public JSEntry newSymbol(String code) {
		return scope.getExactVar(code);
	}
	
	public JSNamespace namespace(String s) {
		if (namespaces.containsKey(s))
			return namespaces.get(s);
		JSNamespace ret = new JSNamespace(s);
		namespaces.put(s, ret);
		return ret;
	}

	public void toScript(JSBuilder jsb) {
		block.toScript(jsb);
	}

	@Override
	public String toString() {
		JSBuilder builder = getBuilder();
		block.toScript(builder);
		return builder.toString();
	}
}
