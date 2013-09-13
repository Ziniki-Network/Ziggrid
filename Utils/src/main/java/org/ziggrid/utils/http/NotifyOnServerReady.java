package org.ziggrid.utils.http;

import org.ziggrid.utils.serialization.Endpoint;

public interface NotifyOnServerReady {

	void serverReady(InlineServer inlineServer, Endpoint addr);

}
