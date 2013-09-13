package org.ziggrid.utils.exceptions;

import org.ziggrid.utils.xml.XMLElement;

@SuppressWarnings("serial")
public class InvalidXMLTagException extends XMLUtilException implements XMLProcessingException {
	public final XMLElement xe;
	public final String which;
	public final Object callbacks;

	public InvalidXMLTagException(XMLElement xe, String which, Object callbacks) {
		super("The object " + callbacks + " does not have a handler for tag " + which + " in element " + xe);
		this.xe = xe;
		this.which = which;
		this.callbacks = callbacks;
	}

	public String getAttribute() {
		return null;
	}
}
