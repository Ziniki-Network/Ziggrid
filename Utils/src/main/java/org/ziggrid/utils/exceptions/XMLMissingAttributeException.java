package org.ziggrid.utils.exceptions;

@SuppressWarnings("serial")
public class XMLMissingAttributeException extends XMLUtilException implements XMLProcessingException {

	public XMLMissingAttributeException(String message) {
		super(message);
	}

	public String getAttribute() {
		return null;
	}

}
