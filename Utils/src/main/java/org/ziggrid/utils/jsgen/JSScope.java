package org.ziggrid.utils.jsgen;

import java.util.ArrayList;
import java.util.List;

import org.ziggrid.utils.exceptions.UtilException;

public class JSScope {
	private final JSScope parent;
	private final List<JSVar> vars = new ArrayList<JSVar>();

	public JSScope(JSScope parent) {
		this.parent = parent;
	}

	public LValue resolveClass(String name) {
		JSVar ret = new JSVar(this, name, true);
		vars.add(ret);
		return ret;
	}

	public JSVar getVarLike(String s) {
		JSVar ret = new JSVar(this, s, false);
		vars.add(ret);
		return ret;
	}

	public JSVar getDefinedVar(String s) {
		for (JSVar v : vars)
			if (v.getName().equals(s))
				return v;
		if (parent != null)
			return parent.getDefinedVar(s);
		throw new UtilException("There is no var " + s);
	}

	public JSVar getExactVar(String s) {
		JSVar ret = new JSVar(this, s, true);
		vars.add(ret);
		return ret;
	}

	public List<JSVar> allScopedVars() {
		List<JSVar> ret = new ArrayList<JSVar>();
		applyVars(ret);
		return ret;
	}

	private void applyVars(List<JSVar> ret) {
		if (parent != null)
			parent.applyVars(ret);
		ret.addAll(vars);
	}
}
