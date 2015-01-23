package org.ziggrid.driver;

import org.zincapi.jsonapi.Payload;

public interface ObserverListener {

	void deliver(Payload payload);

	void sendError(String msg);
}
