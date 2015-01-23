package org.ziggrid.driver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ziggrid.api.ExistingObjectProvider;
import org.ziggrid.kvstore.KVStorageEngine;
import org.ziggrid.kvstore.KVStore;
import org.ziggrid.kvstore.KVTransaction;
import org.ziggrid.kvstore.QueuedItem;
import org.zinutils.metrics.CodaHaleMetrics;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

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
public abstract class ActingProcessor implements ExistingObjectProvider {
	private final List<String> watchables = new ArrayList<String>();
	private final Timer processTimer;

	public ActingProcessor(KVStorageEngine storage, String handlerName) {
		processTimer = CodaHaleMetrics.metrics.timer(handlerName + "QHTimer"); 
	}

	public void addWatchable(String s) {
		watchables.add(s);
	}
	
	public Collection<? extends String> watchables() {
		return watchables;
	}
	
	public void measureProcessMessage(KVTransaction tx, KVStore store, QueuedItem msg) {
		Context processTimerContext = processTimer.time();
		processMessage(tx, store, msg);
		processTimerContext.stop();
	}
	
	protected abstract void processMessage(KVTransaction tx, KVStore store, QueuedItem item);
}
