package org.ziggrid.driver;

import java.io.IOException;

@SuppressWarnings("serial")
public class ObserverDisconnectedException extends RuntimeException {

	public ObserverDisconnectedException(IOException e) {
		super("Disconnected from client", e);
	}

}
