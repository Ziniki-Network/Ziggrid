package org.ziggrid.driver;

import org.ziggrid.taptomq.TapDataPacket;

public interface MessageSource {

	void initialize();

	TapDataPacket getNextMessage();

	void startMessageFlow() throws Exception;

	boolean hasMoreMessages();

	void shutdown();
	
}
