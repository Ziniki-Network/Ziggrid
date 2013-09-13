package org.ziggrid.utils.jsgen;

import java.util.ArrayList;
import java.util.List;

import org.ziggrid.utils.collections.CollectionUtils;
import org.ziggrid.utils.exceptions.UtilException;


public class JSBlock {
	final List<Stmt> stmts = new ArrayList<Stmt>();
	final JSScope scope;
	private final boolean useParens;
	final JSEntry ownedBy;

	JSBlock(JSScope scope, JSEntry ownedBy) {
		this(scope, ownedBy, true);
	}

	public JSBlock(JSScope scope, JSEntry ownedBy, boolean useParens) {
		this.scope = scope;
		this.useParens = useParens;
		this.ownedBy = ownedBy;
	}

	public JSScope getScope() {
		return scope;
	}

	public LValue resolveClass(String name) {
		return scope.resolveClass(name);
	}

	/** For ordering purposes, it is desirable to be able to say at time t, "I may want to put things here"
	 * and then at time t+k to put them there.  This issues such a marker with the appearance of a block.
	 * @return a sub-block of this block
	 */
	public JSBlock marker() {
		JSInnerBlock ret = new JSInnerBlock(scope);
		stmts.add(ret);
		return ret.getBlock();
	}

	public void add(Stmt stmt) {
		stmts.add(stmt);
	}

	public void add(JSExpr voidExpr) {
		stmts.add(new VoidExprStmt(voidExpr));
	}

	public void toScript(JSBuilder sb) {
		toScript(sb, true);
	}
	
	public void toScript(JSBuilder sb, boolean withFinalNL) {
		if (useParens)
			sb.ocb();
		for (Stmt s : stmts)
			s.toScript(sb);
		if (useParens)
			sb.ccb(withFinalNL);
	}


	public boolean isEmpty() {
		return stmts.isEmpty();
	}

	public void continueLoop() {
		add(new JSContinue());
	}

	public VarDecl declareExactVar(String var) {
		JSVar jsvar = scope.getExactVar(var);
		VarDecl ret = new VarDecl(scope, jsvar);
		add(new Assign(jsvar, ret, true));
		return ret;
	}

	public VarDecl declareVarLike(String var, Object expr) {
		JSVar jsvar = scope.getVarLike(var);
		VarDecl ret = new VarDecl(scope, jsvar);
		JSExpr value = null;
		if (expr == null)
			value = new NullExpr();
		else if (expr instanceof JSExpr)
			value = (JSExpr)expr;
		else if (expr instanceof String || expr instanceof Integer)
			value = new JSValue(expr);
		else
			throw new UtilException("Cannot handle value of type " + expr.getClass());
		add(new Assign(jsvar, value, true));
		return ret;
	}

	public VarDecl declareVarLike(String var) {
		JSVar jsvar = scope.getVarLike(var);
		VarDecl ret = new VarDecl(scope, jsvar);
		add(new Assign(jsvar, ret, true));
		return ret;
	}

	public void assign(ArrayIndex a, JSExpr expr) {
		add(new Assign(a, expr));
	}

	public void assign(LValue m, JSExpr expr) {
		add(new Assign(m, expr));
	}

	public void assign(JSVar v, ArrayIndex expr) {
		add(new Assign(v, expr, false));
	}

	public JSExprGenerator assign(LValue v) {
		JSExprGenerator ret = new JSExprGenerator(scope);
		add(new Assign(v, ret));
		return ret;
	}

	public IfElseStmt ifelse() {
		IfElseStmt stmt = new IfElseStmt(scope);
		add(stmt);
		return stmt;
	}

	public ForPropsStmt forProps(String takes, LValue fields) {
		ForPropsStmt ret = new ForPropsStmt(scope, takes, fields);
		add(ret);
		return ret;
	}

	public FunctionCall voidCall(JSVar jsVar) {
		FunctionCall toAdd = new FunctionCall(scope, jsVar);
		add(toAdd);
		return toAdd;
	}

	public FunctionCall voidCall(String name, JSExpr... args) {
		FunctionCall toAdd = new FunctionCall(scope, name);
		for (JSExpr e : args)
			toAdd.arg(e);
		add(toAdd);
		return toAdd;
	}

	public MethodCall voidMethod(JSExpr target, String name, JSExpr... args) {
		MethodCall toAdd = new MethodCall(scope, target, name);
		for (JSExpr e : args)
			toAdd.arg(e);
		add(toAdd);
		return toAdd;
	}

	public void voidStmt(JSExpr expr) {
		add(new VoidExprStmt(expr));
	}

	public JSExprGenerator voidStmt() {
		JSExprGenerator ret = new JSExprGenerator(scope);
		add(new VoidExprStmt(ret));
		return ret;
	}

	public ForEachStmt forEach(String var, JSExpr over) {
		ForEachStmt ret = new ForEachStmt(scope, var, over);
		add(ret);
		return ret;
	}

	public JSFunction createFunction(String name, String... args) {
		JSFunction ret = newFunction(args);
		if (name != null) {
			ret.giveName(name);
			add(ret);
		}
		return ret;
	}

	public JSFunction newFunction(String... args) {
		return newFunction(CollectionUtils.listOf(args));
	}

	private JSFunction newFunction(List<String> args) {
		JSFunction ret = new JSFunction(scope, args);
		return ret;
	}

	public JSExpr value(String cvar) {
		return new JSValue(cvar);
	}

	public void returnVoid() {
		add(new JSReturn());
	}

	public JSVar mapType(String name) {
		return scope.getExactVar(name);
	}
}
