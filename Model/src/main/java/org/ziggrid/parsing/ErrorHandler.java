package org.ziggrid.parsing;

import java.util.ArrayList;
import java.util.List;

import org.ziggrid.model.Definition;

public class ErrorHandler {
	public final List<String> errors = new ArrayList<String>();

	public void report(Definition d, String message) {
		if (d != null)
			errors.add(message + " when " + d.context());
		else
			errors.add(message);
	}

	public boolean displayErrors() {
		if (errors.isEmpty())
			return false;
		for (String e : errors)
			System.out.println(e);
		return true;
	}

	public boolean hasErrors() {
		return !errors.isEmpty();
	}

}
