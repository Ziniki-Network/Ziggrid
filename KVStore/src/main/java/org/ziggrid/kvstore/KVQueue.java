package org.ziggrid.kvstore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class KVQueue {
	protected static final Logger logger = LoggerFactory.getLogger("KVQueue");

	public abstract void startMessageFlow();

	public abstract QueuedItem nextMessage(KVTransaction tx);
}
