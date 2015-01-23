package org.ziggrid.model;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.ziggrid.api.Definition;
import org.ziggrid.api.IModel;
import org.ziggrid.exceptions.ZiggridException;
import org.ziggrid.model.ObjectDefinition.KeyElement;
import org.ziggrid.model.ObjectDefinition.KeyField;
import org.ziggrid.parsing.ErrorHandler;
import org.zinutils.collections.ListMap;
import org.zinutils.exceptions.UtilException;
import org.zinutils.graphs.DirectedAcyclicGraph;
import org.zinutils.graphs.Link;
import org.zinutils.graphs.Node;
import org.zinutils.utils.Crypto;
import org.zinutils.utils.PrettyPrinter;

public class Model implements IModel {
	public final ListMap<String, Definition> definitions = new ListMap<String, Definition>();
	private final Map<String,String> shas = new HashMap<String, String>();
	private Set<Definition> validated = new HashSet<Definition>();
	public final DirectedAcyclicGraph<Definition> dag = new DirectedAcyclicGraph<Definition>();
	private boolean validating = false;
	private Set<String> restrictedWatchables;

	@Override
	public boolean restrictionIncludes(String name) {
		return restrictedWatchables == null || restrictedWatchables.contains(name);
	}

	public void setSHA1(String docId, String hash) {
		if (shas.containsKey(docId))
			throw new UtilException("Cannot specify document " + docId + " multiple times");
		shas.put(docId, hash);
	}

	public boolean isSHA(String s, String sha) {
		return shas.containsKey(s) && shas.get(s).equals(sha);
	}

	@Override
	public String getSHA(String doc) {
		return shas.get(doc);
	}

	public String shaFor(Definition d) {
		return shaFor(d, "");
	}
	
	public String shaFor(Definition d, String variation) {
		PrettyPrinter pp = new PrettyPrinter();
		d.prettyPrint(pp);
		pp.append(variation);
		return Crypto.hash(pp.toString());
	}
	
	public void add(ErrorHandler eh, String docId, Definition od) {
		if (od == null)
			return;
		
		if (docId == null)
			throw new ZiggridException("no doc id");
		if (od instanceof ObjectDefinition && definitions.contains(((ObjectDefinition) od).name))
			throw new ZiggridException("Cannot specify duplicate model called " + ((ObjectDefinition) od).name);
		definitions.add(docId, od);
		dag.ensure(od);
	}

	public void prettyPrint(PrintWriter out) {
		PrettyPrinter pp = new PrettyPrinter();
		for (String doc : definitions) {
			pp.append(doc + ":");
			pp.requireNewline();
			for (Definition d : definitions.get(doc))
				d.prettyPrint(pp);
		}
		out.print(pp);
		out.flush();
	}

	public Set<String> documents() {
		Set<String> ret = new HashSet<String>(definitions.keySet());
		ret.remove(null);
		return ret;
	}

	public ObjectDefinition getModel(ErrorHandler eh, String name) {
		ObjectDefinition ret = getObject(eh, name);
		if (ret != null)
			return ret;
		if (validating)
			internalValidate(eh);
		return getObject(eh, name);
	}

	private ObjectDefinition getObject(ErrorHandler eh, String name) {
		for (Definition d : definitions.values()) {
			if (d instanceof ObjectDefinition && ((ObjectDefinition)d).name.equals(name)) {
				if (validating)
					validate(eh, d);
				return (ObjectDefinition)d;
			}
		}
		return null;
	}

	public List<SummaryDefinition> getSummaries(String summary, String event) {
		List<SummaryDefinition> ret = new ArrayList<SummaryDefinition>();
		for (Definition d : definitions.values()) {
			if (d instanceof SummaryDefinition) {
				SummaryDefinition sd = (SummaryDefinition) d;
				if (sd.summary.equals(summary) && sd.event.equals(event))
					ret.add(sd);
			}
		}
		return ret;
	}

	public List<ObjectDefinition> objects(String documentName) {
		return select(documentName, ObjectDefinition.class);
	}

	public List<EnhancementDefinition> enhancers(String documentName) {
		return select(documentName, EnhancementDefinition.class);
	}
	
	public List<SummaryDefinition> summaries(String documentName) {
		return select(documentName, SummaryDefinition.class);
	}

	public List<LeaderboardDefinition> leaderboards(String documentName) {
		return select(documentName, LeaderboardDefinition.class);
	}

	public List<CorrelationDefinition> correlations(String documentName) {
		return select(documentName, CorrelationDefinition.class);
	}

	public List<SnapshotDefinition> snapshots(String documentName) {
		return select(documentName, SnapshotDefinition.class);
	}

	public List<CompositeDefinition> composites(String documentName) {
		return select(documentName, CompositeDefinition.class);
	}

	public List<IndexDefinition> indices(String documentName) {
		return select(documentName, IndexDefinition.class);
	}

	@SuppressWarnings("unchecked")
	private <T> List<T> select(String doc, Class<T> cls) {
		ArrayList<T> ret = new ArrayList<T>();
		for (Definition d : definitions.get(doc))
			if (cls.isInstance(d))
				ret.add((T)d);
		return ret;
	}

	public void validate(ErrorHandler eh) {
		validating = true;
		if (eh.hasErrors())
			return;

		internalValidate(eh);
		validating = false;
	}

	public void internalValidate(ErrorHandler eh) {
		for (Definition d : definitions.values())
			validate(eh, d);
	}

	public void validate(ErrorHandler eh, Definition d) {
		if (validated.contains(d))
			return;
		try {
			validated.add(d);
			if (d instanceof ObjectDefinition) {
				ObjectDefinition od = (ObjectDefinition)d;
				od.complete(eh, this);
			} else if (d instanceof EnhancementDefinition)
				validateEnhancement(eh, (EnhancementDefinition)d);
			else if (d instanceof SummaryDefinition)
				validateSummary(eh, (SummaryDefinition)d);
			else if (d instanceof LeaderboardDefinition)
				validateLeaderboard(eh, (LeaderboardDefinition)d);
			else if (d instanceof CorrelationDefinition)
				validateCorrelation(eh, (CorrelationDefinition)d);
			else if (d instanceof SnapshotDefinition)
				validateSnapshot(eh, (SnapshotDefinition)d);
			else if (d instanceof IndexDefinition)
				validateIndex(eh, (IndexDefinition)d);
			else if (d instanceof CompositeDefinition)
				validateComposite(eh, (CompositeDefinition)d);
			else
				throw new UtilException("Cannot validate " + d);
		} catch (Exception ex) {
			ex.printStackTrace();
			eh.report(d, ex.getMessage());
		}
	}
	
	private void validateEnhancement(ErrorHandler eh, EnhancementDefinition d) {
		dag.ensure(d);
		ObjectDefinition from = getModel(eh, d.from);
		if (from == null)
			eh.report(d, "There is no definition for " + d.from);
		dag.link(d, from);
		ObjectDefinition to = getModel(eh, d.to);
		if (to == null)
			throw new ZiggridException("There is no definition for " + d.to + " when enhancing from " + d.from + " to " + d.to);
		dag.link(to, d);
		
		for (Entry<String, Enhancement> e : d.fields.entrySet()) {
			if (to.getField(e.getKey()) == null)
				eh.report(d, "There is no field " + e.getKey() + " in model " + d.to);
			validateOperation(eh, d, from, e.getValue());
		}
//		if (!definitions.con)
	}

	private void validateOperation(ErrorHandler eh, Definition d, ObjectDefinition from, Enhancement value) {
		if (value instanceof IntegerConstant || value instanceof DoubleConstant || value instanceof StringConstant || value instanceof ListConstant)
			return;
		else if (value instanceof FieldEnhancement) {
			String field = ((FieldEnhancement)value).field;
			if (from.getField(field) == null)
				eh.report(d, "There is no field " + field + " in model " + from.name);
		} else if (value instanceof StringContainsOp) {
			StringContainsOp op = (StringContainsOp) value;
			if (from.getField(op.field) == null)
				eh.report(d, "There is no field " + op.field + " in model " + from.name);
		} else if (value instanceof IfElseOperation) {
			IfElseOperation op = (IfElseOperation) value;
			validateOperation(eh, d, from, op.test);
			validateOperation(eh, d, from, op.ifTrue);
			validateOperation(eh, d, from, op.ifFalse);
		} else if (value instanceof GroupingOperation) {
			GroupingOperation op = (GroupingOperation) value;
			validateOperation(eh, d, from, op.basedOn);
			validateOperation(eh, d, from, op.dividers);
		} else if (value instanceof BinaryOperation) {
			BinaryOperation op = (BinaryOperation) value;
			validateOperation(eh, d, from, op.lhs);
			validateOperation(eh, d, from, op.rhs);
		} else if (value instanceof SumOperation) {
			SumOperation op = (SumOperation) value;
			for (Enhancement x : op.args)
				validateOperation(eh, d, from, x);
		} else
			eh.report(d, "Cannot handle " + value);
	}

	private void validateSummary(ErrorHandler eh, SummaryDefinition d) {
		dag.ensure(d);
		ObjectDefinition event = getModel(eh, d.event);
		if (event == null)
			eh.report(d, "There is no definition for " + d.event);
		dag.link(d, event);
		ObjectDefinition summary = getModel(eh, d.summary);
		if (summary == null)
			throw new ZiggridException("There is no definition for " + d.summary + " when reducing " + d.event + " into " + d.summary);
		dag.link(summary, d);
		for (int i=0;i<d.matches.size();i++) {
			String rname = d.summary + "-key" + i;
			ObjectDefinition rolledUp = new ObjectDefinition(d.docId, rname);
			for (int j=0;j<i;j++)
				rolledUp.addField(summary.getField(d.matches.get(j).summaryField));
			for (FieldDefinition f : summary.fields())
				if (!f.isKey)
					rolledUp.addField(f);
			add(eh, summary.docId, rolledUp);
			dag.link(rolledUp, d);
		}
		for (MatchField e : d.matches) {
			if (summary.getField(e.summaryField) == null)
				eh.report(d, "There is no field " + e.summaryField + " in model " + d.summary);
			if (event.getField(e.eventField) == null)
				eh.report(d, "There is no field " + e.eventField + " in model " + d.event);
//			validateOperation(eh, d, event, e.getValue());
		}
		for (Entry<String, Reduction> r : d.reductions.entrySet()) {
			if (summary.getField(r.getKey()) == null)
				eh.report(d, "There is no field " + r.getKey() + " in model " + d.summary);
//			if (event.getField(r.getValue()..eventField) == null)
//				eh.report(d, "There is no field " + e.eventField + " in model " + d.event);
		}
	}

	private void validateLeaderboard(ErrorHandler eh, LeaderboardDefinition d) {
		dag.ensure(d);
		ObjectDefinition about = getModel(eh, d.from);
		if (about == null)
			eh.report(d, "There is no definition for " + d.from);
		else {
			dag.link(d, about);
		}
		d.complete(eh, this);
		for (Grouping x : d.groupings()) {
			ObjectDefinition main = getModel(eh, d.getViewName(x));
			ObjectDefinition entry = getModel(eh, d.getEntryName(x));
			dag.link(main, d);
			dag.link(entry, d);
		}
	}

	private void validateIndex(ErrorHandler eh, IndexDefinition d) {
		dag.ensure(d);
		d.complete(eh, this);
		ObjectDefinition idx = getModel(eh, d.ofType);
		if (idx == null)
			eh.report(d, "There is no definition for " + d.ofType);
		else
			dag.link(d, idx);
	}

	private void validateCorrelation(ErrorHandler eh, CorrelationDefinition d) {
		dag.ensure(d);
		d.complete(eh, this);
		ObjectDefinition about = getModel(eh, d.from);
		if (about == null)
			eh.report(d, "There is no definition for " + d.from);
		else
			dag.link(d, about);
		for (Grouping x: d.groupings()) {
			ObjectDefinition into = getModel(eh, d.getViewName(x));
			if (into == null)
				eh.report(d, "There is no definition for " + d.getViewName(x));
			else
				dag.link(into, d);
		}
	}

	private void validateSnapshot(ErrorHandler eh, SnapshotDefinition d) {
		dag.ensure(d);
		d.complete(eh, this);
		ObjectDefinition about = getModel(eh, d.from);
		if (about == null)
			eh.report(d, "There is no definition for " + d.from);
		else
			dag.link(d, about);
		ObjectDefinition produce = getModel(eh, d.getViewName());
		if (produce == null)
			eh.report(d, "There is no definition for " + d.getViewName());
		else
			dag.link(produce, d);
	}

	private void validateComposite(ErrorHandler eh, CompositeDefinition d) {
		dag.ensure(d);
		ObjectDefinition from = getModel(eh, d.from);
		if (from == null)
			eh.report(d, "There is no definition for " + d.from);
		else
			dag.link(d, from);
		ObjectDefinition into = getModel(eh, d.into);
		if (into == null)
			eh.report(d, "There is no definition for " + d.into);
		else
			dag.link(into, d);
		if (from == null || into == null)
			return;
		
		for (KeyElement ke : d.keys)
			if (ke instanceof KeyField) {
				String field = ((KeyField)ke).field;
				if (from.getField(field) == null)
					eh.report(d, "The type " + d.from + " has no field " + field);
			}
		for (NamedEnhancement vf : d.fields) {
			if (into.getField(vf.name) == null)
				eh.report(d, "The type " + d.into + " has no field " + vf.name);
			validateOperation(eh, d, from, vf.enh);
		}
	}

	@Override
	public Set<Definition> willProcess(String type) {
		// TODO Auto-generated method stub
		ErrorHandler eh = new ErrorHandler();
		ObjectDefinition d = getModel(eh, type);
		if (d == null)
			throw new UtilException("Could not find definition for " + type);
		Node<Definition> node = dag.find(d);
		Set<Link<Definition>> linksTo = node.linksTo();
		Set<Definition> ret = new HashSet<Definition>();
		for (Link<Definition> l : linksTo) {
			ret.add(l.getFromNode().getEntry());
		}
		return ret;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (String s : definitions.keySet()) {
			sb.append(s + " => " + definitions.get(s));
		}
		return sb.toString();
	}

	public void selectProcessorsFor(String want) {
		if (restrictedWatchables == null)
			restrictedWatchables = new TreeSet<String>();
		for (Definition d : findProcessorsFor(want)) {
			if (d instanceof ObjectDefinition)
				restrictedWatchables.add(((ObjectDefinition)d).name);
		}
	}

	private List<Definition> findProcessorsFor(String want) {
		Definition d = null;
		for (String doc : documents()) {
			for (Definition q : definitions.get(doc))
				if (q instanceof ObjectDefinition && ((ObjectDefinition)q).name.equals(want)) {
					d = q;
					break;
				}
		}
		if (d == null)
			throw new UtilException("There is no definition for " + want);
		List<Definition> ret = new ArrayList<Definition>();
		Node<Definition> n = dag.find(d);
		traverse(ret, n);
		return ret;
	}

	private void traverse(List<Definition> ret, Node<Definition> n) {
		if (ret.contains(n.getEntry()))
			return;
		ret.add(n.getEntry());
		for (Link<Definition> l :  n.linksFrom())
			traverse(ret, l.getToNode());
	}
}
