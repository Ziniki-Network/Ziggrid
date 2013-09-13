package org.ziggrid.utils.xml;

// This is much more specific to what I'm doing than it should be for a generic
// XML library.  Try and pull out what I'm doing into its own thing.
public abstract class IoCContext {
	private final XMLElement elt;

	public IoCContext(XMLElement elt) {
		this.elt = elt;
	}

	public XMLElement element() {
		return elt;
	}
}
