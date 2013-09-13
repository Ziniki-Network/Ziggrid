package org.ziggrid.utils.exceptions;

import org.xml.sax.SAXParseException;

@SuppressWarnings("serial")
public class XMLParseException extends RuntimeException implements XMLProcessingException {

	public XMLParseException(SAXParseException ex) {
		super(ex);
	}

	public String getAttribute() {
		return null;
	}

}
