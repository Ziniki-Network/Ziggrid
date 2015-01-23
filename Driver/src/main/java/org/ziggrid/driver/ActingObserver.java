package org.ziggrid.driver;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.api.StorageEngine;
import org.ziggrid.driver.interests.InterestEngine;
import org.ziggrid.kvstore.KVDatabase;
import org.ziggrid.kvstore.KVStorageEngine;
import org.ziggrid.kvstore.KVTransaction;
import org.ziggrid.kvstore.QueuedItem;
import org.ziggrid.model.Model;
import org.zinutils.sync.SyncUtils;

import com.foundationdb.FDBException;
import com.foundationdb.async.Function;

/** An "ActingObserver" is an implmentation of a ZiggridObserver which actively reads
 * queues and attempts to perform the relevant action.
 * 
 * An ActingObserver exists in exactly one thread and basically "owns" that.  It may, however,
 * be configured to "read" multiple queues and do multiple different actions on that.
 * 
 * The overall system configuration should be designed so that at no point do two threads/actingobservers
 * ever try to read the same queue.  The idea is that the queues so perfectly partition the space to write
 * into that no two queue entries will write to the same object.
 *
 * <p>
 * &copy; 2014 Gareth Powell.  All rights reserved.
 *
 * @author Gareth Powell
 *
 */
public class ActingObserver implements ZiggridObserver {
	protected static final Logger logger = LoggerFactory.getLogger("Observer");
	protected final Model model;
	private final Map<String, ActingProcessor> processors;
	private final InterestEngine interests;
	private final KVStorageEngine storage;
	private final KVDatabase db;
	private final int liveFor;
	private final int txGroupSize;
//	private int max = -1;
	private boolean done;
	private TreeSet<String> watchables = new TreeSet<String>();
	private final ActingQueueHandler aqh;

	public ActingObserver(Model model, StorageEngine store, Map<String, ActingProcessor> processors, int liveFor, int txGroupSize, InterestEngine interests, int fromQ, int toQ, int range) {
		this.model = model;
		this.processors = processors;
		this.txGroupSize = txGroupSize;
		this.interests = interests;
		this.storage = (KVStorageEngine)store;
		this.db = storage.database();
		this.liveFor = liveFor;
		this.aqh = new ActingQueueHandler(storage, db.queueFrom(fromQ, toQ, range));
	}

	@Override
	public void run() {
		try {
			long lastActive = new Date().getTime();
			while (!done() || (liveFor == 0 || new Date().getTime() < lastActive + liveFor)) {
				Date now = new Date();
				int delay = aqh.isReadyAt(now);
				if (delay > 0) {
//					logger.info("Sleeping for " + delay);
					SyncUtils.sleep(delay);
					continue;
				}
				
//				// Now try this queue in a single transaction
				Function<KVTransaction, QueuedItem> operate = new Function<KVTransaction, QueuedItem>() {
					@Override
					public QueuedItem apply(KVTransaction tx) {
						QueuedItem msg = aqh.nextMessage(tx);
						if (msg == null) return null;
						if (!processors.containsKey(msg.procSHA)) {
							logger.error("There is no processor for " + msg.procSHA);
							return null;
						}
						ActingProcessor proc = processors.get(msg.procSHA);
						logger.debug("Processing " + msg.qid + " with " + proc);
						proc.measureProcessMessage(tx, storage.getStoreForTx(tx), msg);
//							if (max != -1 && max-- <= 0) {
//								logger.info("Reached maximum number of records; draining and exiting");
//								break;
//							}
						return msg;
					}
				};
				logger.trace("beginning tx");
				boolean didSomething = false;
				KVTransaction tx = db.beginTx();
				QueuedItem currItem = null;
				try {
					for (int i=0;i<txGroupSize;i++) {
						// reset these for debugging purposes
						currItem = null;
						currItem = operate.apply(tx);
						if (i==0 && currItem != null)
							logger.debug(Thread.currentThread().getName() + " operating in tx");
						if (currItem == null)
							break;
						didSomething = true;
					}
					if (didSomething)
						logger.debug("Acted in tx ... committing");
					logger.trace("Committing tx");
					tx.commit();

					// If it did something, make it ready again ...
					if (didSomething) {
						lastActive = new Date().getTime();
						interests.process(tx.updatedObjects());
					}
				} catch (FDBException ex) {
					if (currItem != null)
						logger.error("Transaction failed for processor sha " + currItem.procSHA + " in queue " + aqh + " with key " + currItem.qid + ": " + ex.toString());
					else
						logger.error("Observer transaction failed " + ex.toString());
//					if (ex.getCode() == 1020 || (ex.getCode() != 1007 && ex.getCode() != 1004 && !tx.isRetryable())) // Quit on conflict or utter failure only
//						System.exit(1);
					tx.rollback();
				}
				
			}
		} catch (Exception ex) {
			logger.error("Observer thread died.", ex);
		} finally {
			close();
			logger.info("Observer for " + aqh + " exiting");
		}
	}

	public boolean done() {
		return done;
	}

	@Override
	public Model getModel() {
		return model;
	}

	@Override
	public Set<String> getWatchables() {
		return watchables;
	}

	@Override
	public boolean watch(ObserverListener lsnr, int obs, String table, Map<String, Object> constraints) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void unwatch(int hs) {
		// TODO Auto-generated method stub

	}

	@Override
	public void close() {
		done = true;
	}
	
	@Override
	public String toString() {
		return "ActingObserver{" + this.aqh + "}";
	}
}
