package org.ziggrid.utils.jsgen;

import org.ziggrid.utils.exceptions.UtilException;

public class JSVar extends LValue {
	private final JSScope scope;
	private final String var;
	private final boolean exact;

	JSVar(JSScope scope, String var, boolean exact) {
		this.scope = scope;
		this.var = var;
		this.exact = exact;
	}

	@Override
	public void toScript(JSBuilder sb) {
		if (exact) {
			sb.ident(var);
			return;
		}
		int idx = 0;
		int count = 0;
		for (JSVar v : scope.allScopedVars()) {
			if (v.var.equals(var)) {
				++count;
				if (v.equals(this))
					idx = count;
			}
		}
		if (count == 0)
			throw new UtilException("Couldn't find var " + this + " in its own scope!");
		else if (count == 1)
			sb.ident(var);
		else
			sb.ident(var+idx);
	}

	public String getName() {
		return var;
	}
	
	@Override
	public String toString() {
		return var;
	}
}
