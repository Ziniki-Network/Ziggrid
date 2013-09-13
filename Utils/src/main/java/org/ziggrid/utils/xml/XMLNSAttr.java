package org.ziggrid.utils.xml;

import org.w3c.dom.Element;

public class XMLNSAttr {
	private final String namespaceURI;
	private final String qualifiedName;

	XMLNSAttr(String namespaceURI, String qualifiedName)
	{
		this.namespaceURI = namespaceURI;
		this.qualifiedName = qualifiedName;
	}
	
	public void applyTo(Element elt, String value) {
		elt.setAttributeNS(namespaceURI, qualifiedName, value);
	}

}
