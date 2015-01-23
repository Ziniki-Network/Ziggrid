package org.ziggrid.model;

import java.util.ArrayList;
import java.util.List;

import org.ziggrid.api.Definition;
import org.ziggrid.exceptions.ZiggridException;
import org.ziggrid.parsing.ErrorHandler;
import org.zinutils.utils.PrettyPrinter;

public class SnapshotDefinition implements Definition {
	private final ObjectDefinition underlying;
	public final String name;
	public final String from;
	public final List<NamedEnhancement> group = new ArrayList<NamedEnhancement>();
	public final List<NamedEnhancement> values = new ArrayList<NamedEnhancement>();
	public NamedEnhancement upTo;
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

	public void groupBy(NamedEnhancement e) {
		group.add(e);
	}

	public void upTo(NamedEnhancement u) {
		upTo = u;
	}

	public void decay(Decay d) {
		decay = d;
	}

	public void value(NamedEnhancement e) {
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
		for (NamedEnhancement e : group)
			underlying.addField(e.fieldDefinition(ofModel));
		underlying.addField(upTo.fieldDefinition(ofModel));
		for (NamedEnhancement e : values) {
			underlying.addField(e.fieldDefinition(ofModel));
		}
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
		for (NamedEnhancement s : group) {
			pp.append(sep);
			s.enh.prettyPrint(pp);
			sep = ", ";
		}
		pp.append(";");
		pp.requireNewline();
		pp.append("up to ");
		upTo.enh.prettyPrint(pp);
		pp.append(";");
		pp.requireNewline();
		if (decay != null) {
			decay.prettyPrint(pp);
			pp.requireNewline();
		}
		for (NamedEnhancement vf : values) {
			pp.append(vf.name);
			pp.append(" = ");
			vf.enh.prettyPrint(pp);
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

