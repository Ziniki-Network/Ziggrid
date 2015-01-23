package org.ziggrid.driver;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.kvstore.KVQueue;
import org.ziggrid.kvstore.KVStorageEngine;
import org.ziggrid.kvstore.KVTransaction;
import org.ziggrid.kvstore.QueuedItem;


/** In an "active" world (such as Memory or Foundation) it is necessary for the ActingObservers
 * to continually poll the incoming queues for something to do.
 * 
 * When they find something to do, they need to process it.
 * 
 * This class represents the base class of all such queue-based, active observers.  Each actual,
 * individually threaded "ActingObserver" is responsible for handling multiple of these queues.
 *
 * <p>
 * &copy; 2014 Gareth Powell.  All rights reserved.
 *
 * @author Gareth Powell
 *
 */
public class ActingQueueHandler implements Comparable<ActingQueueHandler> {
	protected static final Logger logger = LoggerFactory.getLogger("QueueHandler");
	private final KVQueue queue;
	private long blockedUntil;
	private int backoff;
	private int uniqueOrder;
	
	public ActingQueueHandler(KVStorageEngine storage, KVQueue queue) {
		this.queue = queue;
	}

	public QueuedItem nextMessage(KVTransaction tx) {
		QueuedItem ret = null;
		try {
			ret = queue.nextMessage(tx);
		} catch (Exception ex) {
			// TODO: remove this temporary debugging statement
			ex.printStackTrace();
			logger.error("Encountered exception: " + ex.toString());
		}
		if (ret == null)
			backoff(new Date());
		else
			blockedUntil = 0;
		return ret;
	}

	public void backoff(Date now) {
		if (blockedUntil == 0)
			backoff = 100;
		else if (backoff < 5000)
			backoff = backoff*2;
		blockedUntil = now.getTime() + backoff; 
	}
	
	public int isReadyAt(Date now) {
		long ret = blockedUntil - now.getTime();
		if (ret <= 0)
			return 0;
		return (int)ret;
	}

	public void setUniqueOrder(int size) {
		uniqueOrder = size;
	}

	@Override
	public int compareTo(ActingQueueHandler o) {
		int ret = Long.valueOf(this.blockedUntil).compareTo(o.blockedUntil);
		if (ret != 0)
			return ret;
		return Integer.compare(uniqueOrder, o.uniqueOrder);
	}

	@Override
	public String toString() {
		return this.getClass().getName() + " " + queue;
	}
}
