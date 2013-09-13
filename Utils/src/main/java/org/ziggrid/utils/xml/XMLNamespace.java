package org.ziggrid.utils.xml;

public class XMLNamespace {
	private String qualifier;
	private String uri;

	XMLNamespace(String ns, String url) {
		qualifier = ns;
		uri = url;
	}

	public XMLNSAttr attr(String field) {
		return new XMLNSAttr(uri, qualifier + ":" + field);
	}

}
