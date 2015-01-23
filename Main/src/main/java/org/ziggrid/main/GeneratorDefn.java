package org.ziggrid.main;

import org.zinutils.xml.XML;

public class GeneratorDefn {
	public final XML generatorXML;
	public final int limit;
	public final int genMod;
	public final int genOutOf;

	public GeneratorDefn(XML generatorXML, int limit, int genMod, int genOutOf) {
		this.generatorXML = generatorXML;
		this.limit = limit;
		this.genMod = genMod;
		this.genOutOf = genOutOf;
	}
}
