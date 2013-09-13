package org.ziggrid.utils.exceptions;

@SuppressWarnings("serial")
public class ResourceNotFoundException extends UtilException implements UtilPredictableException {

	public ResourceNotFoundException(String msg) {
		super(msg);
	}

}
