package org.ziggrid.utils.jsgen;

import java.util.ArrayList;
import java.util.List;

import org.ziggrid.utils.exceptions.UtilException;

public class JSFunction extends JSExpr {
	private String name;
	private final List<JSVar> args = new ArrayList<JSVar>();
	private final JSBlock block;
	private final JSScope scope;
	private JSVar var;

	JSFunction(JSScope parent, List<String> args)
	{
		scope = new JSScope(parent);
		block = new JSBlock(scope, this);
		for (String a : args)
			this.args.add(scope.getExactVar(a));
	}

	public JSScope getScope() {
		return scope;
	}

	public void giveName(String name) {
		if (name != null) {
			this.name = name;
			var = new JSVar(scope, name, true);
		}
	}


	public JSExpr asVar() {
		if (var == null)
			throw new UtilException("Must give function a name to use asVar()");
		return var;
	}

	public JSVar addArg(String arg) {
		JSVar ret = scope.getExactVar(arg);
		args.add(ret);
		return ret;
	}

	public JSVar getArg(String a) {
		for (JSVar ai : args)
			if (ai.getName().equals(a))
				return ai;
		throw new UtilException("There is no argument " + a);
	}

	public JSBlock getBlock() {
		return block;
	}

	@Override
	public void toScript(JSBuilder sb) {
		sb.ident("function");
		if (name != null)
			sb.ident(name);
		sb.orb();
		appendArgs(sb);
		sb.crb();
		block.toScript(sb);
	}

	private void appendArgs(JSBuilder sb) {
		String sep = "";
		for (JSVar a : args) {
			sb.append(sep);
			sb.append(a.getName());
			sep = ", ";
		}
	}
	
	@Override
	public String toString() {
		JSBuilder sb = new JSBuilder();
        toScript(sb);
		return sb.toString();
	}
}
