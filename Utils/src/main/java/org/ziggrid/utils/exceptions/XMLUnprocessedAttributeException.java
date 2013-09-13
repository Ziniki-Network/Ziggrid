package org.ziggrid.utils.exceptions;

@SuppressWarnings("serial")
public class XMLUnprocessedAttributeException extends XMLAboutAttributeException implements XMLProcessingException {

	public XMLUnprocessedAttributeException(String attr, String message) {
		super(attr, message);
	}

}
