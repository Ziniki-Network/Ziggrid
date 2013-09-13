package org.ziggrid.model;

import java.util.ArrayList;
import java.util.List;

import org.ziggrid.utils.utils.PrettyPrinter;

public class SumOperation implements Enhancement {
	public final List<Enhancement> args = new ArrayList<Enhancement>();
	public final String op;

	public SumOperation(String op) {
		this.op = op;
		
	}
	
	public void arg(Enhancement arg) {
		args.add(arg);
	}

	@Override
	public void prettyPrint(PrettyPrinter pp) {
		String sep = "";
		for (Enhancement e : args) {
			pp.append(sep);
			sep = " " + op + " ";
			e.prettyPrint(pp);
		}
	}

}
