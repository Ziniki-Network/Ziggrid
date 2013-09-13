package org.ziggrid.exceptions;

@SuppressWarnings("serial")
public class ZiggridException extends RuntimeException {
	
	public ZiggridException(String message) {
		super(message);
	}

	public ZiggridException(String message, Throwable cause) {
		super(message, cause);
	}
}
