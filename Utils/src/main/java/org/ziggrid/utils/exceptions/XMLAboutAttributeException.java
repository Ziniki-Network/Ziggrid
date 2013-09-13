package org.ziggrid.utils.exceptions;

@SuppressWarnings("serial")
public class XMLAboutAttributeException extends XMLUtilException implements XMLProcessingException {
	private final String attr;

	public XMLAboutAttributeException(String attr, String message) {
		super(message);
		this.attr = attr;
	}

	public String getAttribute() {
		return attr;
	}

}
