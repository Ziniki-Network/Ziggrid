package org.ziggrid.parsing;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.ziggrid.api.Definition;
import org.ziggrid.exceptions.ZiggridException;
import org.ziggrid.model.BinaryOperation;
import org.ziggrid.model.CompositeDefinition;
import org.ziggrid.model.CorrelationDefinition;
import org.ziggrid.model.Decay;
import org.ziggrid.model.DoubleConstant;
import org.ziggrid.model.Enhancement;
import org.ziggrid.model.EnhancementDefinition;
import org.ziggrid.model.FieldDefinition;
import org.ziggrid.model.FieldEnhancement;
import org.ziggrid.model.GroupingOperation;
import org.ziggrid.model.IfElseOperation;
import org.ziggrid.model.IndexDefinition;
import org.ziggrid.model.IntegerConstant;
import org.ziggrid.model.LeaderboardDefinition;
import org.ziggrid.model.LinearDecay;
import org.ziggrid.model.ListConstant;
import org.ziggrid.model.Model;
import org.ziggrid.model.NamedEnhancement;
import org.ziggrid.model.ObjectDefinition;
import org.ziggrid.model.OpReductionWithNoFields;
import org.ziggrid.model.OpReductionWithOneField;
import org.ziggrid.model.Reduction;
import org.ziggrid.model.SnapshotDefinition;
import org.ziggrid.model.StringConstant;
import org.ziggrid.model.StringContainsOp;
import org.ziggrid.model.SumOperation;
import org.ziggrid.model.SummaryDefinition;
import org.zinutils.collections.CollectionUtils;
import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.Crypto;
import org.zinutils.utils.FileUtils;

// TODO:
// 1. Start handling errors properly VERY SOON
// 2. Semantic checking (eg. that referenced definitions exist) VERY SOON
// 3. Add some basic unit tests of the model at least VERY SOON

public class JsonReader {
	public Model readModel(ErrorHandler eh, String docId, String json) {
		try {
			Model ret = new Model();
			readModel(eh, ret, docId, json);
			return ret;
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}
	
	public void readModel(ErrorHandler eh, Model ret, String docId, String json) {
		try {
			ret.setSHA1(docId, Crypto.hash(json));
			JSONArray mappingFile = new JSONArray(json);
			for (int i=0;i<mappingFile.length();i++) {
				Definition d = null;
				try {
					JSONObject obj = mappingFile.getJSONObject(i);
					if (obj.has("name"))
						d = readObjectDefinition(eh, docId, obj);
					else if (obj.has("enhanced"))
						d = readEnhancementDefinition(eh, obj);
					else if (obj.has("summary"))
						d = readSummaryDefinition(eh, ret, docId, obj);
					else if (obj.has("leaderboard"))
						d = readLeaderboardDefinition(eh, ret, docId, obj);
					else if (obj.has("correlate"))
						d = readCorrelationDefinition(eh, ret, docId, obj);
					else if (obj.has("snapshot"))
						d = readSnapshotDefinition(eh, ret, docId, obj);
					else if (obj.has("index"))
						d = readIndexDefinition(eh, obj);
					else if (obj.has("composeInto"))
						d = readCompositeDefinition(eh, ret, docId, obj);
					else {
						eh.report(null, "Could not recognize definition: " + obj);
					}
					ret.add(eh, docId, d);
				} catch (JSONException ex) {
					unpick(eh, d, ex);
				}
			}
		} catch (JSONException ex) {
			unpick(eh, null, ex);
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	public void unpick(ErrorHandler eh, Definition d, JSONException ex) {
		// Try and unpick message
		String message = ex.getMessage();
		String atc = " at character ";
		int idx = message.indexOf(atc);
		if (idx != -1) {
			String tmp = message.substring(idx+atc.length());
			int idx2 = tmp.indexOf(' ');
			if (idx2 != -1) {
				String nbr = tmp.substring(0, idx2);
				tmp = tmp.substring(idx2 + " of ".length());
				try {
					int pos = Integer.parseInt(nbr);
					if (pos < tmp.length()) {
						tmp = tmp.substring(pos);
						if (tmp.length() > 35)
							tmp = tmp.substring(0, 35);
						message = message.substring(0, idx) + " at: " + tmp;
						throw new ZiggridException(message);
					}
				} catch (NumberFormatException e2) { }
			}
		}
		eh.report(d, message);
	}

	private ObjectDefinition readObjectDefinition(ErrorHandler eh, String docId, JSONObject obj) throws JSONException {
		assertOnly(eh, null, "Object", obj.keys(), "name", "key", "fields");
		ObjectDefinition od = new ObjectDefinition(docId, obj.getString("name"));
		if (obj.has("key")) {
			JSONArray keys = obj.getJSONArray("key");
			for (int i=0;i<keys.length();i++) {
				Object ki = keys.get(i);
				if (ki instanceof String)
					od.addKeyString((String)ki);
				else if (ki instanceof JSONObject) {
					JSONObject joki = (JSONObject) ki;
					od.addKeyField((String)joki.getString("field"));
				} else
					eh.report(od, "Cannot have key member " + ki);
			}
		}
		JSONArray fields = obj.getJSONArray("fields");
		for (int i=0;i<fields.length();i++) {
			od.addField(readFieldDefinition(eh, fields.getJSONObject(i)));
		}
		return od;
	}

	private FieldDefinition readFieldDefinition(ErrorHandler eh, JSONObject f) throws JSONException {
		assertOnly(eh, null, "Field", f.keys(), "name", "type", "key");
		FieldDefinition ret = new FieldDefinition(f.getString("name"), f.getString("type"), f.optBoolean("key", false));
		return ret;
	}

	private EnhancementDefinition readEnhancementDefinition(ErrorHandler eh, JSONObject obj) throws JSONException {
		assertOnly(eh, null, "Enhancement", obj.keys(), "from", "enhanced", "enhance");
		EnhancementDefinition od = new EnhancementDefinition(obj.getString("from"), obj.getString("enhanced"));
		JSONObject fields = obj.getJSONObject("enhance");
		@SuppressWarnings("unchecked")
		Iterator<String> it = fields.keys();
		while (it.hasNext()) {
			String ef = it.next();
			Object f = fields.get(ef);
			Enhancement enhancement = readEnhancement(eh, od, f);
			od.define(ef, enhancement);
		}
		return od;
	}

	public NamedEnhancement readNamedEnhancement(ErrorHandler eh, Definition d, Object f) throws JSONException {
		if (f == null)
			return null; // error propagation ... avoid cascading
		else if (f instanceof String) {
			String fn = (String)f;
			return new NamedEnhancement(fn, new FieldEnhancement(fn));
		} else if (f instanceof JSONObject) {
			JSONObject obj = (JSONObject) f;
			if (obj.length() != 1) {
				eh.report(d, "There are multiple keys in field enhancement: " + obj);
				return null;
			}
			String fn = (String) obj.keys().next();
			return new NamedEnhancement(fn, readEnhancement(eh, d, obj.get(fn)));
		} else {
			eh.report(d, "Invalid named enhancement definition: " + f);
			return null;
		}
	}

	private Enhancement readEnhancement(ErrorHandler eh, Definition d, Object f) throws JSONException {
		Enhancement enhancement = null;
		if (f == null)
			return null; // error propagation ... avoid cascading
		else if (f instanceof String)
			enhancement = new FieldEnhancement((String)f);
		else if (f instanceof Integer)
			enhancement = new IntegerConstant((Integer)f);
		else if (f instanceof Double)
			return new DoubleConstant((Double)f);
		else if (f instanceof JSONObject) {
			JSONObject of = (JSONObject) f;
			if (of.has("op"))
				enhancement = readEnhanceOp(eh, d, of);
			else if (of.has("number")) {
				assertOnly(eh, d, "number", of.keys(), "number");
				enhancement = new IntegerConstant(of.getInt("number"));
			} else if (of.has("string")) {
				assertOnly(eh, d, "string", of.keys(), "string");
				enhancement = new StringConstant(of.getString("string"));
			}
		}
		if (enhancement == null)
			eh.report(d, "Cannot interpret the enhancement " + f);
		return enhancement;
	}
	
	private Enhancement readEnhanceOp(ErrorHandler eh, Definition d, JSONObject of) throws JSONException {
		try {
			String op = of.getString("op");
			if ("ifelse".equals(op)) {
				assertOnly(eh, d, "ifelse", of.keys(), "op", "test", "true", "false");
				return new IfElseOperation(readEnhancement(eh, d, of.get("test")), readEnhancement(eh, d, of.get("true")), readEnhancement(eh, d, of.get("false")));
			} else if ("group".equals(op)) {
				assertOnly(eh, d, "group", of.keys(), "op", "value", "dividers", "moreThan");
				return new GroupingOperation(readEnhancement(eh, d, of.get("value")), intlist(of.getJSONArray("dividers")), readEnhancement(eh, d, of.get("moreThan")));
			} else if ("+".equals(op) || "*".equals(op)) {
				JSONArray args = of.getJSONArray("args");
				SumOperation ret = new SumOperation(op);
				for (int i=0;i<args.length();i++)
					ret.arg(readEnhancement(eh, d, args.get(i)));
				return ret;
			} else if ("-".equals(op) || "/".equals(op) || op.equals("==") || op.equals(">=")) {
				Enhancement lhs = readEnhancement(eh, d, of.get("lhs"));
				Enhancement rhs = readEnhancement(eh, d, of.get("rhs"));
				BinaryOperation ret = new BinaryOperation(op, lhs, rhs);
				return ret;
//		} else if ("==".equals(op)) {
//			String field = of.getString("field");
//			Enhancement rhs;
//			if (of.has("string")) {
//				assertOnly(eh, d, "==", of.keys(), "op", "field", "string");
//				rhs = new StringConstant(of.getString("string"));
//			} else
//				throw new UtilException("Cannot handle equality: " + of);
//			BinaryOperation ret = new BinaryOperation("==", new FieldEnhancement(field), rhs);
//			return ret;
//		} else if () {
//			Object lhs = of.get("lhs");
//			if (lhs == null) eh.report(d, "Must specify lhs for " + op);
//			Object rhs = of.get("rhs");
//			if (rhs == null) eh.report(d, "Must specify rhs for " + op);
//			if (lhs == null || rhs == null)
//				return null;
//			return new BinaryOperation(op, parseCalculation(eh, d, lhs), parseCalculation(eh, d, rhs));
			} else if ("in".equals(op)) {
				String field = of.getString("field");
				Enhancement rhs;
				if (of.has("strings")) {
					assertOnly(eh, d, "in", of.keys(), "op", "field", "strings");
					rhs = stringlist(of.getJSONArray("strings"));
				} else
					throw new UtilException("Cannot handle in of type: " + of);
				BinaryOperation ret = new BinaryOperation("in", new FieldEnhancement(field), rhs);
				return ret;
			} else if ("stringContains".equals(op)) {
				assertOnly(eh, d, "stringContains", of.keys(), "op", "field", "text");
				return new StringContainsOp(of.getString("field"), of.getString("text"));
			} else {
				eh.report(d, "Cannot handle operation " + op);
				return null;
			}
		} catch (JSONException ex) {
			eh.report(d, "Error parsing " + of + ": " + ex.getMessage());
			return null;
		}
	}

	private Enhancement stringlist(JSONArray jsonArray) throws JSONException {
		ListConstant ret = new ListConstant();
		for (int i=0;i<jsonArray.length();i++)
			ret.add((String)jsonArray.get(i));
		return ret;
	}

	private Enhancement intlist(JSONArray jsonArray) throws JSONException {
		ListConstant ret = new ListConstant();
		for (int i=0;i<jsonArray.length();i++)
			ret.add((Integer)jsonArray.get(i));
		return ret;
	}

	@SuppressWarnings("unchecked")
	private SummaryDefinition readSummaryDefinition(ErrorHandler eh, Model model, String docId, JSONObject obj) throws JSONException {
		assertOnly(eh, null, "Summary", obj.keys(), "summary", "event", "match", "reduce");
		String summary = obj.getString("summary");
		String event = obj.getString("event");
		List<SummaryDefinition> like = model.getSummaries(summary, event);
		SummaryDefinition ret = new SummaryDefinition(docId, summary, event);
		if (!like.isEmpty()) {
			int cnt = like.size();
			if (cnt == 1)
				like.get(0).setWhich(1);
			ret.setWhich(cnt+1);
		}
		JSONObject match = obj.getJSONObject("match");
		for (String matchSummary : CollectionUtils.iterableOf((Iterator<String>)match.keys()))
			ret.match(matchSummary, match.getString(matchSummary));
		JSONObject reduce = obj.getJSONObject("reduce");
		for (String reduceTo : CollectionUtils.iterableOf((Iterator<String>)reduce.keys()))
			ret.reduceTo(reduceTo, parseReduction(reduce.getJSONObject(reduceTo)));
		return ret;
	}


	private Reduction parseReduction(JSONObject r) throws JSONException {
		String op = r.getString("op");
		if ("+=".equals(op)) {
			return new OpReductionWithOneField("+=", r.getString("field"));
		} else if ("++".equals(op)) {
			return new OpReductionWithNoFields("++");
		} else
			throw new UtilException("No such operator: " + op);
	}

	private LeaderboardDefinition readLeaderboardDefinition(ErrorHandler eh, Model model, String docId, JSONObject obj) throws JSONException {
		assertOnly(eh, null, "Leaderboard", obj.keys(), "leaderboard", "from", "groupby", "order", "sortby", "top", "values", "filter");
		LeaderboardDefinition ret = new LeaderboardDefinition(docId, obj.getString("leaderboard"), obj.getString("from"));
		if (obj.has("order")) {
			String o = obj.getString("order");
			if (o.equals("asc"))
				ret.ascending = true;
			else if (o.equals("desc"))
				ret.ascending = false;
			else
				throw new UtilException("Cannot figure ordering from " + o);
		}
		else
			ret.ascending = true;
		if (obj.has("top"))
			ret.top(obj.getInt("top"));
		else
			ret.top(10);
		JSONArray groups = obj.getJSONArray("groupby");
		// TODO: we should check these are distinct
		for (int i=0;i<groups.length();i++) {
			JSONArray jsonArray = groups.getJSONArray(i);
			List<String> g = new ArrayList<String>();
			for (int j=0;j<jsonArray.length();j++)
				g.add(jsonArray.getString(j));
			ret.groupBy(eh, model, g);
		}
		JSONArray sorts = obj.getJSONArray("sortby");
		for (int i=0;i<sorts.length();i++)
			ret.sortBy(readNamedEnhancement(eh, ret, sorts.get(i)));
		JSONArray values = obj.getJSONArray("values");
		for (int i=0;i<values.length();i++)
			ret.returnValue(values.getString(i));
		if (obj.has("filter")) {
			JSONArray filters = obj.getJSONArray("filter");
			for (int i=0;i<filters.length();i++)
				ret.filter(readNamedEnhancement(eh, ret, filters.get(i)));
		}
		return ret;
	}

	private CorrelationDefinition readCorrelationDefinition(ErrorHandler eh, Model model, String docId, JSONObject obj) throws JSONException {
		assertOnly(eh, null, "Correlation", obj.keys(), "correlate", "case", "groupby", "value");
		CorrelationDefinition ret = new CorrelationDefinition(docId, obj.getString("correlate"));
		ret.useValue(readEnhancement(eh, ret, obj.get("value")));
		JSONArray cases = obj.getJSONArray("case");
		for (int i=0;i<cases.length();i++) {
			ret.addCaseItem(readNamedEnhancement(eh, ret, cases.get(i)));
		}
		// TODO: we should check these are distinct
		JSONArray groups = obj.getJSONArray("groupby");
		for (int i=0;i<groups.length();i++) {
			JSONArray jsonArray = groups.getJSONArray(i);
			List<String> g = new ArrayList<String>();
			for (int j=0;j<jsonArray.length();j++)
				g.add(jsonArray.getString(j));
			ret.groupBy(eh, model, g);
		}
		return ret;
	}

	private SnapshotDefinition readSnapshotDefinition(ErrorHandler eh, Model model, String docId, JSONObject obj) throws JSONException {
		assertOnly(eh, null, "Snapshot", obj.keys(), "snapshot", "from", "groupby", "upTo", "decay", "values");
		SnapshotDefinition ret = new SnapshotDefinition(eh, model, docId, obj.getString("snapshot"), obj.getString("from"));
		JSONArray grps = obj.getJSONArray("groupby");
		for (int i=0;i<grps.length();i++) {
			ret.groupBy(readNamedEnhancement(eh, ret, grps.get(i)));
		}
		ret.upTo(readNamedEnhancement(eh, ret, obj.get("upTo")));
		if (obj.has("decay"))
			ret.decay(parseDecay(eh, ret, obj.getJSONObject("decay")));
		JSONArray values = obj.getJSONArray("values");
		for (int i=0;i<values.length();i++) {
			ret.value(readNamedEnhancement(eh, ret, values.get(i)));
		}
		return ret;
	}

	private IndexDefinition readIndexDefinition(ErrorHandler eh, JSONObject obj) throws JSONException {
		assertOnly(eh, null, "Leaderboard", obj.keys(), "index", "from", "groupby", "values");
		IndexDefinition ret = new IndexDefinition(obj.getString("index"), obj.getString("from"));
		JSONArray groups = obj.getJSONArray("groupby");
		// TODO: we should check these are distinct
		for (int i=0;i<groups.length();i++) {
			JSONArray jsonArray = groups.getJSONArray(i);
			List<String> g = new ArrayList<String>();
			for (int j=0;j<jsonArray.length();j++)
				g.add(jsonArray.getString(j));
			ret.groupBy(g);
		}
		JSONArray values = obj.getJSONArray("values");
		for (int i=0;i<values.length();i++)
			ret.returnValue(values.getString(i));
		return ret;
	}

	private Definition readCompositeDefinition(ErrorHandler eh, Model model, String docId, JSONObject obj) throws JSONException {
		assertOnly(eh, null, "Composition", obj.keys(), "composeInto", "from", "key", "fields");
		CompositeDefinition ret = new CompositeDefinition(obj.getString("composeInto"), obj.getString("from"));
		JSONArray keys = obj.getJSONArray("key");
		for (int i=0;i<keys.length();i++) {
			Object ki = keys.get(i);
			if (ki instanceof String)
				ret.addKeyString((String)ki);
			else if (ki instanceof JSONObject) {
				JSONObject joki = (JSONObject) ki;
				ret.addKeyField((String)joki.getString("field"));
			} else
				eh.report(ret, "Cannot have key member " + ki);
		}
		JSONArray fields = obj.getJSONArray("fields");
		for (int i=0;i<fields.length();i++) {
			ret.addField(readNamedEnhancement(eh, ret, fields.get(i)));
		}
		return ret;
	}

	private Decay parseDecay(ErrorHandler eh, SnapshotDefinition ret, JSONObject obj) throws JSONException {
		if (!obj.has("method")) {
			eh.report(ret, "Decay must have a method");
			return null;
		}
		String method = obj.getString("method");
		if (method.equals("linear")) {
			assertOnly(eh, ret, "Decay", obj.keys(), "method", "expiry");
			int expiry = obj.getInt("expiry"); 
			return new LinearDecay(expiry);
		}
		return null;
	}

	private void assertOnly(ErrorHandler eh, Definition defn, String objType, @SuppressWarnings("rawtypes") Iterator keys, String... allowed) {
		List<String> l = CollectionUtils.listOf(allowed);
		while (keys.hasNext()) {
			String k = (String) keys.next();
			if (!l.contains(k))
				eh.report(defn, objType + " definitions may not include the key: '" + k + "'");
		}
	}

	public static void main(String[] args) {
		String txt = FileUtils.readFile(new File("../SampleData/buckets/ziggrid-baseball/baseball/model.json"));
		JsonReader jr = new JsonReader();
		ErrorHandler eh = new ErrorHandler();
		Model model = jr.readModel(eh, "baseball", txt);
		model.validate(eh);
		if (eh.displayErrors())
			return;
		model.prettyPrint(new PrintWriter(System.out));
	}
}
