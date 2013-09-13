package org.ziggrid.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.ziggrid.exceptions.ZiggridException;
import org.ziggrid.parsing.ErrorHandler;
import org.ziggrid.utils.utils.PrettyPrinter;

public class SnapshotDefinition implements Definition {
	private final ObjectDefinition underlying;
	final String name;
	public final String from;
	public final List<Enhancement> group = new ArrayList<Enhancement>();
	public final List<String> valueFields = new ArrayList<String>();
	public final List<Enhancement> values = new ArrayList<Enhancement>();
	public Enhancement upTo;
	private Decay decay;

	public SnapshotDefinition(ErrorHandler eh, Model model, String docId, String name, String from) {
		this.name = "snapshot_" + name;
		this.from = from;
		underlying = new ObjectDefinition(docId, getViewName());
		underlying.setCompleter(this);
		model.add(eh, docId, underlying);
	}

	public String getViewName() {
		return name;
	}

	@Override
	public String context() {
		return "creating snapshot " + name + " of " + from;
	}

	public void groupBy(Enhancement e) {
		group.add(e);
	}

	public void upTo(Enhancement e) {
		upTo = e;
	}

	public void decay(Decay d) {
		decay = d;
	}

	public void value(String name, Enhancement e) {
		valueFields.add(name);
		values.add(e);
	}

	public double startFrom(Object object) {
		if (decay == null)
			return 0;
		return decay.startFrom(object);
	}

	public double figureDecay(int endAt, Object object) {
		if (decay == null)
			return 1;
		return decay.after(endAt, object);
	}

	public void complete(ErrorHandler eh, Model m) {
		ObjectDefinition ofModel = m.getModel(eh, from);
		if (ofModel == null)
			throw new ZiggridException("There is no model " + from);
		for (Enhancement e : group)
			handle(ofModel, e);
		handle(ofModel, upTo);
		for (int i=0;i<valueFields.size();i++) {
			Enhancement e = values.get(i);
			String type;
			if (e instanceof FieldEnhancement) {
				String fname = ((FieldEnhancement)e).field;
				type = ofModel.getField(fname).type;
			} else
				type = "number";
			underlying.addField(new FieldDefinition(valueFields.get(i), type, false));
		}
	}

	public void handle(ObjectDefinition ofModel, Enhancement e) {
		if (e instanceof FieldEnhancement) {
			String fname = ((FieldEnhancement)e).field;
			underlying.addField(new FieldDefinition(fname, ofModel.getField(fname).type, true));
		} else
			System.out.println("Cannot handle enhancement " + e);
	}

	@Override
	public void prettyPrint(PrettyPrinter pp) {
		pp.append("snapshot ");
		pp.append(name);
		pp.append(" from ");
		pp.append(from);
		pp.append(" {");
		pp.indentMore();
		pp.append("group by ");
		String sep = "";
		for (Enhancement s : group) {
			pp.append(sep);
			s.prettyPrint(pp);
			sep = ", ";
		}
		pp.append(";");
		pp.requireNewline();
		pp.append("up to ");
		upTo.prettyPrint(pp);
		pp.append(";");
		pp.requireNewline();
		if (decay != null) {
			decay.prettyPrint(pp);
			pp.requireNewline();
		}
		Iterator<Enhancement> vi = values.iterator();
		for (String vf : valueFields) {
			pp.append(vf);
			pp.append(" = ");
			vi.next().prettyPrint(pp);
			pp.append(";");
			pp.requireNewline();
		}
		pp.indentLess();
		pp.append("}");
		pp.requireNewline();
	}

	@Override
	public String toString() {
		return "Snapshot of " + from + " as " + name;
	}
}

