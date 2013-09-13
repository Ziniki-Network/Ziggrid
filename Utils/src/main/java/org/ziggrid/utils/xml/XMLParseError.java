package org.ziggrid.utils.xml;

import org.xml.sax.SAXException;

@SuppressWarnings("serial")
public class XMLParseError extends RuntimeException {
	public final Location from;
	public final Location to;
	public final SAXException ex;
	
	public XMLParseError(Location from, Location to, SAXException ex) {
		super(ex.getMessage());
		this.from = from;
		this.to = to;
		this.ex = ex;
	}
}
