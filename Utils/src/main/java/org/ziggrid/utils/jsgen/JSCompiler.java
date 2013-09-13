package org.ziggrid.utils.jsgen;

public abstract class JSCompiler extends JSExpr {
	private JSBlock block;
	private JSVar loopVar;

	public JSCompiler(AbstractForStmt forLoop) {
		this.block = forLoop.nestedBlock();
		this.loopVar = forLoop.getLoopVar();
		compile();
	}

	public JSCompiler(JSBlock block) {
		this.block = block;
		compile();
	}

	void changeBlock(JSBlock other) { this.block = other; }
	protected JSBlock getBlock() { return block; }
	protected JSVar getLoopVar() { return loopVar; }
	
	public AbstractForStmt arrayIterator(String control, JSExpr array) {
		ForEachStmt ret = new ForEachStmt(block.scope, control, array);
		block.stmts.add(ret);
		return ret;
	}
	
	public void assign(JSVar var, JSExpr rvalue) {
		block.stmts.add(new Assign(var, rvalue, false));
	}

	public void assign(LValue member, JSExpr rvalue) {
		block.stmts.add(new Assign(member, rvalue));
	}

	public void assignWith(String op, LValue member, JSExpr rvalue) {
		block.stmts.add(new Assign(op, member, rvalue));
	}

	public JSExpr binop(String op, JSExpr lhs, JSExpr rhs) {
		return new BinaryOp(block.scope, op).arg(lhs).arg(rhs);
	}

	public void blockComment(String text) {
		block.stmts.add(new JSBlockComment(text));
	}
	
	public void breakOut() {
		block.stmts.add(new JSBreak());
	}

	public void continueLoop() {
		block.stmts.add(new JSContinue());
	}

	public JSFunction declareFunction(String name, String... args) {
		JSFunction decl = block.createFunction(name, args);
		return decl;
	}

	public VarDecl declareVarLike(String var, JSExpr expr) {
		return block.declareVarLike(var, expr);
	}

	public VarDecl declareVar(String var, JSExpr expr) {
		VarDecl decl = declareExactVar(var);
		decl.value(expr);
		return decl;
	}

	public VarDecl declareExactVar(String var) {
		return block.declareExactVar(var);
	}

	public FunctionCall functionExpr(String function, JSExpr... args) {
		FunctionCall expr = new FunctionCall(block.scope, function);
		for (JSExpr arg : args)
			expr.arg(arg);
		return expr;
	}

	public FunctionCall functionExpr(JSExpr fn, JSExpr... args) {
		FunctionCall expr = new FunctionCall(block.scope, fn);
		for (JSExpr arg : args)
			expr.arg(arg);
		return expr;
	}

	public IfElseStmt ifEq(JSExpr lhs, JSExpr rhs) {
		IfElseStmt ret = new IfElseStmt(block.scope);
		ret.test = new BinaryOp("==", lhs, rhs);
		block.stmts.add(ret);
		return ret;
	}
	
	public IfElseStmt ifEEq(JSExpr lhs, JSExpr rhs) {
		IfElseStmt ret = new IfElseStmt(block.scope);
		ret.test = new BinaryOp("===", lhs, rhs);
		block.stmts.add(ret);
		return ret;
	}

	public IfElseStmt ifNEq(JSExpr lhs, JSExpr rhs) {
		IfElseStmt ret = new IfElseStmt(block.scope);
		ret.test = new BinaryOp("!=", lhs, rhs);
		block.stmts.add(ret);
		return ret;
	}
	
	public IfElseStmt ifInstance(JSExpr expr, String type) {
		IfElseStmt ret = new IfElseStmt(block.scope);
		ret.test = new BinaryOp("instanceof", expr, new JSName(type));
		block.stmts.add(ret);
		return ret;
	}
	
	public IfElseStmt ifTruthy(JSExpr expr) {
		IfElseStmt ret = new IfElseStmt(block.scope);
		ret.test = expr;
		block.stmts.add(ret);
		return ret;
	}
	
	public IfElseStmt ifFalsy(JSExpr expr) {
		IfElseStmt ret = new IfElseStmt(block.scope);
		ret.test = new UnaryOp("!", expr);
		block.stmts.add(ret);
		return ret;
	}

	public JSExpr integer(int k) {
		return new JSValue(k);
	}


	public JSExpr doubleValue(double value) {
		return new JSValue(value);
	}

	public FunctionCall jquery(String s) {
		return functionExpr("$", string(s));
	}

	public JSListExpr list(JSExpr... elts) {
		JSListExpr ret = new JSListExpr(block.scope);
		for (JSExpr e : elts)
			ret.add(e);
		return ret;
	}

	public MethodCall methodExpr(String callOn, String method, JSExpr... args) {
		return methodExpr(block.scope.getDefinedVar(callOn), method, args);
	}
	
	public MethodCall methodExpr(JSExpr callOn, String method, JSExpr... args) {
		JSExprGenerator gen = new JSExprGenerator(block.scope);
		MethodCall expr = gen.methodCall(callOn, method);
		for (JSExpr arg : args)
			expr.arg(arg);
		return expr;
	}

	public JSObjectExpr objectHash() {
		JSExprGenerator gen = new JSExprGenerator(block.scope);
		return gen.literalObject();
	}
	
	public AbstractForStmt objectIterator(String control, LValue obj) {
		ForPropsStmt ret = new ForPropsStmt(block.scope, control, obj);
		block.stmts.add(ret);
		return ret;
	}
	
	public JSExpr postOp(LValue value, String op) {
		return new PostOp(op, value);
	}

	public void returnValue(JSExpr expr) {
		block.stmts.add(new JSReturn(expr));
	}
	
	public void returnVoid() {
		block.stmts.add(new JSReturn());
	}

	public JSExpr parens(JSExpr sc) {
		return new JSParens(sc);
	}

	public JSExpr string(String s) {
		return new JSValue(s);
	}

	public JSVar var(String var) {
		return block.scope.getDefinedVar(var);
	}
	
	public void voidExpr(JSExpr expr) {
		block.stmts.add(new VoidExprStmt(expr));
	}
	
	public void voidFunction(String function, JSExpr... args) {
		block.stmts.add(new VoidExprStmt(functionExpr(function, args)));
	}

	public void voidFunction(JSExpr function, JSExpr... args) {
		block.stmts.add(new VoidExprStmt(functionExpr(function, args)));
	}

	public void voidMethod(String callOn, String method, JSExpr... args) {
		block.stmts.add(new VoidExprStmt(methodExpr(callOn, method, args)));
	}

	public void voidMethod(JSExpr callOn, String method, JSExpr... args) {
		block.stmts.add(new VoidExprStmt(methodExpr(callOn, method, args)));
	}

	public JSExpr This() {
		return new JSThis(block.scope);
	}

	public abstract void compile();

	public JSScope getScope() {
		return block.scope;
	}
	
	@Override
	public void toScript(JSBuilder sb) {
		if (block.ownedBy != null)
			block.ownedBy.toScript(sb);
	}
}
