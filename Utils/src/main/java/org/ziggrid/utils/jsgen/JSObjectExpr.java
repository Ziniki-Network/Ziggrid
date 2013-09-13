package org.ziggrid.utils.jsgen;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.ziggrid.utils.collections.CollectionUtils;
import org.ziggrid.utils.exceptions.UtilException;

public class JSObjectExpr extends JSExpr {
	public abstract class JSObjectDefiner extends JSExpr {
		public JSObjectDefiner() {
			members();
		}

		protected JSScope getScope() {
			return scope;
		}
		
		protected JSMember fromThis(String var) {
			return new JSMember(new JSThis(scope), var);
		}
		
		protected void declareVar(String var, JSExpr expr) {
			JSExprGenerator decl = JSObjectExpr.this.var(var);
			decl.value(expr);
		}

		protected JSFunction method(String name, String... args) {
			return JSObjectExpr.this.method(name, args);
		}

		@Override
		public void toScript(JSBuilder sb) {
			JSObjectExpr.this.toScript(sb);
		}
		
		public abstract void members();
	}

	private Map<String, JSEntry> members = new LinkedHashMap<String, JSEntry>();
	private final JSScope scope;
	private final boolean createNested;
	
	JSObjectExpr(JSScope scope, boolean createNested) {
		this.scope = scope;
		this.createNested = createNested;
	}

	public JSObjectExpr marker() {
		JSObjectExpr jsObjectExpr = new JSObjectExpr(scope, false);
		members.put("_"+members.size(), jsObjectExpr);
		return jsObjectExpr;
	}

	public JSFunction method(String name, String... args) {
		return method(name, CollectionUtils.listOf(args));
	}

	public JSFunction method(String name, List<String> args) {
		if (members.containsKey(name))
			throw new UtilException("Cannot define duplicate member " + name);
		JSFunction ret = new JSFunction(scope, args);
		members.put(name, ret);
		return ret;
	}

	public void var(String name, JSExpr expr) {
		if (members.containsKey(name))
			throw new UtilException("Cannot define duplicate member " + name);
		members.put(name, expr);
	}

	public JSExprGenerator var(String name) {
		JSExprGenerator ret = new JSExprGenerator(scope);
		members.put(name, ret);
		return ret;
	}

	@Override
	public void toScript(JSBuilder sb) {
		if (createNested)
			sb.ocb();
		for (Map.Entry<String, JSEntry> kv : members.entrySet()) {
			if (kv.getValue() instanceof JSObjectExpr && !((JSObjectExpr)kv.getValue()).createNested) {
				kv.getValue().toScript(sb);
				continue;
			}
			sb.fieldName(kv.getKey());
			kv.getValue().toScript(sb);
			sb.objectComma();
		}
		if (createNested)
			sb.ccb();
	}
}
