package org.ziggrid.parsing;

import org.ziggrid.exceptions.ZiggridException;

public class ProcessingMethods {
	public boolean enhanceWithView = false;
	public boolean summarizeWithView = false;
	public boolean correlateWithView = false;
	public boolean snapshotWithView = false;
	
	public void useView(String s) {
		if (s.equals("enhance"))
			enhanceWithView = true;
		else if (s.equals("summary"))
			summarizeWithView = true;
		else if (s.equals("correlate"))
			correlateWithView = true;
		else if (s.equals("snapshot"))
			snapshotWithView = true;
		else
			throw new ZiggridException("Cannot understand processor: " + s);
	}

}
