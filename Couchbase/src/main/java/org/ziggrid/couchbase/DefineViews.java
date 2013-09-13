package org.ziggrid.couchbase;

import java.io.File;
import org.apache.commons.httpclient.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.exceptions.ZiggridException;
import org.ziggrid.model.BinaryOperation;
import org.ziggrid.model.Definition;
import org.ziggrid.model.DoubleConstant;
import org.ziggrid.model.Enhancement;
import org.ziggrid.model.FieldDefinition;
import org.ziggrid.model.FieldEnhancement;
import org.ziggrid.model.Grouping;
import org.ziggrid.model.GroupingOperation;
import org.ziggrid.model.IfElseOperation;
import org.ziggrid.model.IndexDefinition;
import org.ziggrid.model.IntegerConstant;
import org.ziggrid.model.LeaderboardDefinition;
import org.ziggrid.model.ListConstant;
import org.ziggrid.model.Model;
import org.ziggrid.model.ObjectDefinition;
import org.ziggrid.model.OpReductionWithNoFields;
import org.ziggrid.model.OpReductionWithOneField;
import org.ziggrid.model.Reduction;
import org.ziggrid.model.StringConstant;
import org.ziggrid.model.StringContainsOp;
import org.ziggrid.model.SumOperation;
import org.ziggrid.parsing.ErrorHandler;
import org.ziggrid.parsing.JsonReader;
import org.ziggrid.utils.exceptions.UtilException;
import org.ziggrid.utils.jsgen.AbstractForStmt;
import org.ziggrid.utils.jsgen.IfElseStmt;
import org.ziggrid.utils.jsgen.JSCompiler;
import org.ziggrid.utils.jsgen.JSExpr;
import org.ziggrid.utils.jsgen.JSListExpr;
import org.ziggrid.utils.jsgen.JSParens;
import org.ziggrid.utils.jsgen.JSValue;
import org.ziggrid.utils.jsgen.JSVar;
import org.ziggrid.utils.jsgen.NullExpr;
import org.ziggrid.utils.jsgen.VarDecl;
import org.ziggrid.utils.sync.SyncUtils;
import org.ziggrid.utils.utils.FileUtils;

public class DefineViews {
	private static final Logger logger = LoggerFactory.getLogger("DefineViews");

	public static void main(String[] args) {
        if (args.length < 4) {
			System.err.println("Usage: loadViews couchUrl bucket documentName ziggridSpec");
			System.exit(1);
		}
		String couchUrl = args[0];
		String bucket = args[1];
		String documentName = args[2];
		String txt;
		File f = new File(args[3]);
		if (f.exists()) {
			txt = FileUtils.readFile(f);
		} else
			txt = FileUtils.readResource("/" + bucket + "/" + documentName + "/" + args[3]);
		JsonReader jr = new JsonReader();
		ErrorHandler eh = new ErrorHandler();
		Model model = jr.readModel(eh, documentName, txt);
		model.validate(eh);
		if (eh.displayErrors())
			return;
		
		new DefineViews().loadDesignDocument(eh, couchUrl + bucket + "/", model, documentName);
	}
	
	
	public void loadDesignDocument(final ErrorHandler eh, String couchUrl, Model model, String documentName) {
//		model.prettyPrint(new PrintWriter(System.out));
		CouchDocument doc = new CouchDocument(documentName);
		/* We don't need this in "by hand" mode, and it will waste valuable CPU cycles
		for (final EnhancementDefinition e : model.enhancers(documentName)) {
			CouchView view = doc.create(e.getViewName());
			final ObjectDefinition fromModel = model.getModel(eh, e.from);
			final ObjectDefinition toModel = model.getModel(eh, e.to);
			new JSCompiler(view.needMap().getBlock()) {
				public void compile() {
					ifNEq(var("meta").member("type"), string("json")).yes.returnVoid();
					ifFalsy(var("doc").member("ziggridType")).yes.returnVoid();
					ifNEq(var("doc").member("ziggridType"), string(e.from)).yes.returnVoid();
					JSListExpr fields = list();
					for (Entry<String, Enhancement> f : e.fields.entrySet()) {
						Enhancement er = f.getValue();
						final JSExpr expr = handleSimpleEnhancementCases(this, f.getKey(), er, fromModel, toModel);
						final JSVar myf = declareVarLike(f.getKey(), expr).getVar();
						if (expr instanceof NullExpr)
							handleComplexEnhancementCases(eh, e, this, f.getKey(), er, fromModel, toModel, myf);
						fields.add(myf);
					}
					voidFunction("emit", binop("+", string(e.to + "_from_"), var("doc").member("id")), fields);
				}
			};
		}
		for (final SummaryDefinition s : model.summaries(documentName)) {
			final CouchView view = doc.create(s.getViewName());
			new JSCompiler(view.needMap().getBlock()) {
				public void compile() {
					ifNEq(var("meta").member("type"), string("json")).yes.returnVoid();
					ifFalsy(var("doc").member("ziggridType")).yes.returnVoid();
					ifNEq(var("doc").member("ziggridType"), string(s.event)).yes.returnVoid();

					// build a key
					JSListExpr key = list();
					for (MatchField f : s.matches) {
						key.add(var("doc").member(f.eventField));
					}

					JSListExpr value = list();
					for (final Entry<String, Reduction> r : s.reductions.entrySet()) {

						// build a value
						Reduction rr = r.getValue();
						final JSExpr expr = handleSimpleReductionCases(this, rr);
						if (expr instanceof NullExpr)
							throw new UtilException("Complex Case not handled " + rr);
						value.add(expr);
					}
					voidFunction("emit", key, value);
				}
			};

			new JSCompiler(view.needReduce().getBlock()) {
				public void compile() {
//					JSVar key = var("key");
					JSVar values = var("values");
					final JSVar rereduce = var("rereduce");
					JSListExpr init = list();
					for (final Entry<String, Reduction> r : s.reductions.entrySet())
						init.add(initialFor(r.getValue()));
					final JSVar ret = declareVarLike("ret", init).getVar();
					final AbstractForStmt loop = arrayIterator("row", values);
					new JSCompiler(loop.nestedBlock()) {
						public void compile() {
							ifFalsy(rereduce).new YesNo() {
								// The basic case
								public void yes() {
									int idx = 0;
									for (final Entry<String, Reduction> r : s.reductions.entrySet()) {
										doReductionFor(this, ret, loop.getLoopVar(), r.getValue(), idx++);
									}									
								}

								// The re-reduce case
								public void no() {
									int idx = 0;
									for (final Entry<String, Reduction> r : s.reductions.entrySet()) {
										doReductionFor(this, ret, loop.getLoopVar(), r.getValue(), idx++);
									}									
								}
							};
						}
					};
					returnValue(ret);
				}
			};
		}
		*/
		for (final LeaderboardDefinition l : model.leaderboards(documentName)) {
			for (final Grouping g : l.groupings()) {
				final Grouping grouping = g;
				CouchView view = doc.create(l.getViewName(grouping));
				new JSCompiler(view.needMap().getBlock()) {
					public void compile() {
						ifNEq(var("meta").member("type"), string("json")).yes.returnVoid();
						ifFalsy(var("doc").member("ziggridType")).yes.returnVoid();
						ifNEq(var("doc").member("ziggridType"), string(l.from)).yes.returnVoid();
	
						// Apply any filters before generating row in result view
						for (Enhancement f : l.filters) {
							JSExpr expr = doEnhancement(eh, l, this, f);
							ifFalsy(expr).yes.returnVoid();
						}
						
						// build a key
						JSListExpr key = list();
						for (String f : grouping.fields) {
							key.add(var("doc").member(f));
						}
						for (Enhancement f : l.sorts) {
							JSExpr expr = doEnhancement(eh, l, this, f);
							key.add(expr);
						}
	
						// build a value
						JSExpr value;
						if (l.values.size() == 0)
							value = new NullExpr();
						else if (l.values.size() == 1)
							value = var("doc").member(l.values.get(0));
						else {
							JSListExpr listValue = list();
							for (String v : l.values)
								listValue.add(var("doc").member(v));
							value = listValue;
						}
						voidFunction("emit", key, value);
					}
				};
				view.reduceCount();
			}
		}
		/* Not in "Local" mode
		for (final CorrelationDefinition cd : model.correlations(documentName)) {
			// 1. Define the "global correlation"
			{
				CouchView view = doc.create(cd.getGlobalViewName());
				new JSCompiler(view.needMap().getBlock()) {
					public void compile() {
						ifNEq(var("meta").member("type"), string("json")).yes.returnVoid();
						ifFalsy(var("doc").member("ziggridType")).yes.returnVoid();
						ifNEq(var("doc").member("ziggridType"), string(cd.from)).yes.returnVoid();
						JSListExpr key = list();
						for (Enhancement f : cd.items) {
							JSExpr expr = doEnhancement(eh, cd, this, f);
							key.add(expr);
						}
						voidFunction("emit", key, doEnhancement(eh, cd, this, cd.value));
					}
				};
				view.reduceStats();
			}
			
			// 2. Now define grouped correlations
			for (final Grouping g : cd.groupings()) {
				final Grouping grouping = g;
				CouchView view = doc.create(cd.getViewName(grouping));
				new JSCompiler(view.needMap().getBlock()) {
					public void compile() {
						ifNEq(var("meta").member("type"), string("json")).yes.returnVoid();
						ifFalsy(var("doc").member("ziggridType")).yes.returnVoid();
						ifNEq(var("doc").member("ziggridType"), string(cd.from)).yes.returnVoid();
	
						JSListExpr key = list();
						for (String f : grouping.fields) {
							key.add(var("doc").member(f));
						}
						for (Enhancement f : cd.items) {
							JSExpr expr = doEnhancement(eh, cd, this, f);
							key.add(expr);
						}
						voidFunction("emit", key, doEnhancement(eh, cd, this, cd.value));
					}
				};
				view.reduceStats();
			}
		}
		for (final SnapshotDefinition sd : model.snapshots(documentName)) {
			CouchView view = doc.create(sd.getViewName());
			new JSCompiler(view.needMap().getBlock()) {
				public void compile() {
					ifNEq(var("meta").member("type"), string("json")).yes.returnVoid();
					ifFalsy(var("doc").member("ziggridType")).yes.returnVoid();
					ifNEq(var("doc").member("ziggridType"), string(sd.from)).yes.returnVoid();

					/*
					// Apply any filters before generating row in result view
					for (Enhancement f : sd.filters) {
						JSExpr expr = doEnhancement(eh, sd, this, f);
						ifFalsy(expr).yes.returnVoid();
					}
					* /
					
					// build a key
					JSListExpr key = list();
					for (Enhancement f : sd.group) {
						JSExpr expr = doEnhancement(eh, sd, this, f);
						key.add(expr);
					}
					key.add(doEnhancement(eh, sd, this, sd.upTo));

					// build a value
					JSListExpr value = list();
					for (Enhancement v : sd.values) {
						value.add(doEnhancement(eh, sd, this, v));
					}
					voidFunction("emit", key, value);
				}
			};
			view.reduceCount();
		}
		*/
		for (final IndexDefinition id : model.indices(documentName)) {
			for (final Grouping g : id.groupings()) {
				final Grouping grouping = g;
				CouchView view = doc.create(id.getViewName(grouping));
				new JSCompiler(view.needMap().getBlock()) {
					public void compile() {
						ifNEq(var("meta").member("type"), string("json")).yes.returnVoid();
						ifFalsy(var("doc").member("ziggridType")).yes.returnVoid();
						ifNEq(var("doc").member("ziggridType"), string(id.ofType)).yes.returnVoid();
	
						// build a key
						JSListExpr key = list();
						for (String f : grouping.fields) {
							key.add(var("doc").member(f));
						}
	
						// build a value
						JSExpr value;
						if (id.values.size() == 0)
							value = new NullExpr();
						else if (id.values.size() == 1)
							value = var("doc").member(id.values.get(0));
						else {
							JSListExpr listValue = list();
							for (String v : id.values)
								listValue.add(var("doc").member(v));
							value = listValue;
						}
						voidFunction("emit", key, value);
					}
				};
				view.reduceCount();
			}
		}
		if (eh.displayErrors())
			return;
		
		int retries = 8;
		while (retries-- >= 0)
			try {
				HttpClient cli = new HttpClient();
				doc.writeToCouch(cli, couchUrl);
				break;
			} catch (UtilException ex) {
				logger.error("Encountered error while processing JavaScript: " + ex.getMessage());
				break;
			} catch (NullPointerException ex) {
				logger.error("Encountered error while processing JavaScript: " + ex.getMessage());
				break;
			} catch (Exception ex) {
				logger.error("Failed to write " + doc.getName() + " to " + couchUrl);
				SyncUtils.sleep(250);
			}
		if (retries < 0)
			throw new UtilException("Could not write " + doc.getName() + " to " + couchUrl);
	}

	private JSExpr doEnhancement(final ErrorHandler eh, final Definition defn, JSCompiler jsc, Enhancement f) {
		if (f instanceof FieldEnhancement)
			return jsc.var("doc").member(((FieldEnhancement)f).field);
		else if (f instanceof IntegerConstant)
			return jsc.integer(((IntegerConstant)f).value);
		else if (f instanceof DoubleConstant)
			return jsc.doubleValue(((DoubleConstant)f).value);
		else if (f instanceof BinaryOperation) {
			BinaryOperation op = (BinaryOperation) f;
			return jsc.binop(op.op, jsc.parens(doEnhancement(eh, defn, jsc, op.lhs)), jsc.parens(doEnhancement(eh, defn, jsc, op.rhs)));
		} else if (f instanceof SumOperation) {
			SumOperation op = (SumOperation) f;
			JSExpr lhs = null;
			for (Enhancement x : op.args) {
				JSExpr sc = doEnhancement(eh, defn, jsc, x);
				if (sc instanceof NullExpr)
					throw new UtilException("Complex cases not supported: " + x);
				if (lhs == null)
					lhs = jsc.parens(sc);
				else
					lhs = jsc.binop(op.op, lhs, jsc.parens(sc));
			}
			if (lhs == null)
				return new JSValue(0);
			else
				return lhs;
		} else if (f instanceof IfElseOperation) {
			final IfElseOperation op = (IfElseOperation) f;
			JSExpr test = doEnhancement(eh, defn, jsc, op.test);
			final VarDecl ret = jsc.declareVarLike("expr", null);
			jsc.ifTruthy(test).new YesNo() {
				public void yes() {
					assign(ret.getVar(), doEnhancement(eh, defn, this, op.ifTrue));
				}

				public void no() {
					assign(ret.getVar(), doEnhancement(eh, defn, this, op.ifFalse));
				}
			};
			return ret.getVar();
		} else {
			eh.report(defn, "Cannot handle enhancement " + f.getClass().getName());
			return null;
		}
	}

	public JSExpr handleSimpleEnhancementCases(JSCompiler jsc, String field, Enhancement er, ObjectDefinition fromModel, ObjectDefinition toModel) {
		if (er instanceof FieldEnhancement)
			return jsc.var("doc").member(((FieldEnhancement)er).field);
		else if (er instanceof IntegerConstant)
			return new JSValue(((IntegerConstant)er).value);
		else if (er instanceof StringConstant)
			return new JSValue(((StringConstant)er).value);
		else if (er instanceof ListConstant) {
			JSListExpr ret = jsc.list();
			for (Object s : (ListConstant) er) {
				if (s instanceof String)
					ret.add(jsc.string((String) s));
				else if (s instanceof Integer)
					ret.add(jsc.integer((Integer) s));
				else
					throw new ZiggridException("Cannot handle " + s + " in lists");
			}
			return ret;
		} else if (er instanceof SumOperation) {
			SumOperation op = (SumOperation) er;
			JSExpr lhs = null;
			for (Enhancement x : op.args) {
				JSExpr sc = handleSimpleEnhancementCases(jsc, field, x, fromModel, toModel);
				if (sc instanceof NullExpr)
					throw new UtilException("Complex cases not supported: " + x);
				if (lhs == null)
					lhs = jsc.parens(sc);
				else
					lhs = jsc.binop(op.op, lhs, jsc.parens(sc));
			}
			if (lhs == null)
				return new JSValue(0);
			else
				return lhs;
		} else if (er instanceof BinaryOperation) {
			BinaryOperation op = (BinaryOperation) er;
			JSExpr lhs = handleSimpleEnhancementCases(jsc, field, op.lhs, fromModel, toModel);
			JSExpr rhs = handleSimpleEnhancementCases(jsc, field, op.rhs, fromModel, toModel);
			return jsc.binop(op.op, jsc.parens(lhs), jsc.parens(rhs));
		} else if (er instanceof StringContainsOp) {
			StringContainsOp op = (StringContainsOp)er;
			JSExpr basicTest = jsc.binop("&&", jsc.functionExpr("typeof", jsc.var("doc").member(op.field)), jsc.binop("!=", jsc.methodExpr(jsc.var("doc").member(op.field), "indexOf", jsc.string(op.text)), new JSValue(-1)));
			FieldDefinition fd = toModel.getField(field);
			if (fd.type.equals("number"))
				basicTest = jsc.binop("+", jsc.integer(0), new JSParens(basicTest));
			return basicTest;
		}
		return new NullExpr();
	}

	public void handleComplexEnhancementCases(final ErrorHandler eh, final Definition defn, final JSCompiler jsc, final String f, Enhancement er, final ObjectDefinition fromModel, final ObjectDefinition toModel, final JSVar myf) {
		if (er instanceof IfElseOperation) {
			final IfElseOperation op = (IfElseOperation) er;
			evaluateEnhancementTest(jsc, f, op.test, fromModel, toModel).new YesNo() {
				public void yes() {
					handleAnyEnhancementCase(eh, defn, this, op.ifTrue, fromModel, toModel, myf, f);
				}

				public void no() {
					handleAnyEnhancementCase(eh, defn, this, op.ifFalse, fromModel, toModel, myf, f);
				}
			};
		} else if (er instanceof GroupingOperation) {
			final GroupingOperation op = (GroupingOperation) er;
			final VarDecl valueTmp = jsc.declareVar("goTmp", null);
			handleAnyEnhancementCase(eh, defn, jsc, op.basedOn, fromModel, toModel, valueTmp.getVar(), f);
			final VarDecl loopOver = jsc.declareVar("loopOver", null);
			handleAnyEnhancementCase(eh, defn, jsc, op.dividers, fromModel, toModel, loopOver.getVar(), f);
			final AbstractForStmt loop = jsc.arrayIterator("maxValue", loopOver.getVar());
			final JSVar loopVar = loop.getLoopVar();
			new JSCompiler(loop.nestedBlock()) {
				public void compile() {
					IfElseStmt exitIfDone = ifTruthy(jsc.binop("<=", valueTmp.getVar(), loopVar));
					new JSCompiler(exitIfDone.yes) {
						public void compile() {
							assign(myf, loopVar);
							breakOut();
						}
					};
				}
			};
			new JSCompiler(jsc.ifEq(myf, null).yes) {
				public void compile() {
					handleAnyEnhancementCase(eh, defn, this, op.moreThan, fromModel, toModel, myf, f);
				}
			};
		} else {
			eh.report(defn, "Cannot enhance " + er.getClass().getName());
		}
	}

	private void handleAnyEnhancementCase(ErrorHandler eh, Definition defn, JSCompiler jsc, Enhancement er, ObjectDefinition fromModel, ObjectDefinition toModel, JSVar myf, String f) {
		JSExpr simple = handleSimpleEnhancementCases(jsc, f, er, fromModel, toModel);
		if (!(simple instanceof NullExpr)) {
			jsc.assign(myf, simple);
			return;
		}
		handleComplexEnhancementCases(eh, defn, jsc, f, er, fromModel, toModel, myf);
	}

	private IfElseStmt evaluateEnhancementTest(JSCompiler jsc, String f, Enhancement test, ObjectDefinition fromModel, ObjectDefinition toModel) {
		if (test instanceof FieldEnhancement) {
			// the field needs to be a boolean for this to work (or use JS truthy)
			return jsc.ifTruthy(jsc.var("doc").member(((FieldEnhancement)test).field));
		} else if (test instanceof BinaryOperation) {
			BinaryOperation t1 = (BinaryOperation) test;
			if (t1.op.equals("=="))
				return jsc.ifEq(jsc.var("doc").member(((FieldEnhancement)t1.lhs).field), handleSimpleEnhancementCases(jsc, f, t1.rhs, fromModel, toModel));
			else if (t1.op.equals("in"))
				return jsc.ifNEq(jsc.methodExpr(handleSimpleEnhancementCases(jsc, f, t1.rhs, fromModel, toModel), "indexOf", jsc.var("doc").member(((FieldEnhancement)t1.lhs).field)), jsc.integer(-1));
			throw new UtilException("Op not handled: " + t1.op);
		} else {
			throw new UtilException("Test not handled: " + test.getClass());
		}
	}
	

	public JSExpr handleSimpleReductionCases(JSCompiler jsc, Reduction r) {
		if (r instanceof OpReductionWithOneField)
			return jsc.var("doc").member(((OpReductionWithOneField)r).eventField);
		else if (r instanceof OpReductionWithNoFields)
			return jsc.integer(1);
		else
			return new NullExpr();
	}

	/*
	private JSExpr initialFor(Reduction reduction) {
		if (reduction instanceof OpReductionWithOneField) {
			String op = ((OpReductionWithOneField) reduction).op;
			if ("+=".equals(op))
				return new JSValue(0);
			else
				throw new UtilException("Cannot handle op: " + op);
		} else if (reduction instanceof OpReductionWithNoFields) {
			String op = ((OpReductionWithNoFields) reduction).op;
			if ("++".equals(op))
				return new JSValue(0);
			else
				throw new UtilException("Cannot handle op: " + op);
		}
		else
			throw new UtilException("Cannot handle reduction: " + reduction);
	}

	private void doReductionFor(JSCompiler jsc, final JSVar ret, final JSVar loopVar, final Reduction r, final int idx) {
		new UseCompiler(jsc) {
			@Override
			public void compile() {
				if (r instanceof OpReductionWithOneField) {
					String op = ((OpReductionWithOneField) r).op;
					if ("+=".equals(op))
						assignWith("+=", ret.subscript(idx), loopVar.subscript(idx));
					else
						throw new UtilException("Cannot handle op: " + op);
				} else if (r instanceof OpReductionWithNoFields) {
					String op = ((OpReductionWithNoFields) r).op;
					if ("++".equals(op))
						assignWith("+=", ret.subscript(idx), loopVar.subscript(idx));
					else
						throw new UtilException("Cannot handle op: " + op);
				}
				else
					throw new UtilException("Cannot handle reduction: " + r);
			}
		};
	}
	*/
}
