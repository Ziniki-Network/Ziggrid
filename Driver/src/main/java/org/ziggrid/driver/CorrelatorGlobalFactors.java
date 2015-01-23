package org.ziggrid.driver;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.ziggrid.kvstore.KVStorageEngine;
import org.ziggrid.kvstore.KVTransaction;
import org.zinutils.exceptions.UtilException;
import org.zinutils.sync.SyncUtils;

import com.foundationdb.FDBException;

/** This class exists because the "global" factors, by their very nature, are global
 * and thus at high risk of contention, continually rolling back transactions on all nodes.
 * 
 * To make matters worse, the more threads you have, the more contention you get, so the
 * slower it goes.
 * 
 * The idea here is to remove the source of contention by keeping a "node-local" copy of the
 * correlation map in memory (thus for a single node system, there should be NO database 
 * contention, ever). Local contention on that node is resolved using standard java
 * synchronization primitives.
 * 
 * The inter-node contention is handled by having a dedicated
 * thread here responsible for all the READING and WRITING of the global correlation map,
 * and recognizing that our updates can be applied in any order as long as we maintain the
 * correct delta information.  This thread is also responsible for ensuring the in-memory
 * version is continually "up to date". 
 *
 * <p>
 * &copy; 2014 Gareth Powell.  All rights reserved.
 *
 * @author Gareth Powell
 *
 */
public class CorrelatorGlobalFactors {
	public class FactorValue {
		public final int occurred;
		public final double total;

		public FactorValue(int occurred, double total) {
			this.occurred = occurred;
			this.total = total;
		}
	}

	public class CGFactor {
		private final String id;
		private int actualOccurred;
		private int stagedOccurred;
		private int unstagedOccurred;
		private double actualTotal;
		private double stagedTotal;
		private double unstagedTotal;
		
		public CGFactor(String id) {
			this.id = id;
		}

		public synchronized FactorValue get() {
			return new FactorValue(actualOccurred + stagedOccurred + unstagedOccurred,  actualTotal + stagedTotal + unstagedTotal);
		}

		public synchronized void record(double outcome) {
			unstagedOccurred++;
			unstagedTotal += outcome;
			if (get().occurred == 0)
				throw new UtilException("Huh?");
		}

		private synchronized FactorValue stage() {
			stagedOccurred = unstagedOccurred;
			unstagedOccurred = 0;
			stagedTotal = unstagedTotal;
			unstagedTotal = 0;
			if (get().occurred == 0)
				throw new UtilException("Huh?");
			return new FactorValue(stagedOccurred, stagedTotal);
		}
		
		public synchronized void commit(int occurred, double total) {
			stagedOccurred = 0;
			stagedTotal = 0;
			actualOccurred = occurred;
			actualTotal = total;
			if (get().occurred == 0)
				throw new UtilException("Huh?");
		}

		public synchronized void rollback() {
			unstagedOccurred += stagedOccurred;
			unstagedTotal += stagedTotal;
			stagedOccurred = 0;
			stagedTotal = 0;
			if (get().occurred == 0)
				throw new UtilException("Huh?");
		}
	}

	private final Map<String, CGFactor> currentMap = new HashMap<String, CGFactor>();
	private final LinkedHashSet<CGFactor> workToDo = new LinkedHashSet<CGFactor>();
	private final Thread workProcessor;

	public CorrelatorGlobalFactors(final KVStorageEngine store, final String sha) {
		workProcessor = new Thread() {
			public void run() {
				int delay = 10;
				while (true) {
					try {
						CGFactor curr;
						synchronized (workToDo) {
							if (workToDo.isEmpty()) {
								SyncUtils.waitFor(workToDo, delay);
								delay = Math.min(delay*2, 5000);
								continue;
							}
							delay = 10;
							curr = workToDo.iterator().next();
							workToDo.remove(curr);
						}
						FactorValue fv = curr.stage();
						String gid = "cgf/" + sha + "/" + curr.id;
//						System.out.println("processing " + gid);
						boolean willCommit = false;
						KVTransaction tx = null;
						try {
							tx = store.database().beginTx();
							int occurred = fv.occurred;
							double total = fv.total;
							if (tx.containsValue(gid+".total")) {
								occurred += (Integer)tx.getValue(gid+".occurred");
								total += (Double)tx.getValue(gid+".total");
							}
							tx.putValue(gid+".occurred", occurred);
							tx.putValue(gid+".total", total);
							willCommit = true;
							tx.commit();
							curr.commit(occurred, total);
						} catch (FDBException ex) {
							ActingCorrelator.logger.error("Conflict occurred trying to write " + gid);
							if (!willCommit && tx != null)
								tx.rollback();
							curr.rollback();
							// add it back to the end of the queue
							synchronized (workToDo) {
								workToDo.add(curr);
							}
						}
					} catch (Throwable t) {
						t.printStackTrace();
					}
				}
			}
		};
		workProcessor.setName("CGF-" + sha);
		workProcessor.setDaemon(true);
		workProcessor.start();
	}

	public void record(String globid, double outcome) {
		CGFactor curr;
		boolean recorded = false;
		synchronized (currentMap) {
			curr = currentMap.get(globid);
			if (curr == null) {
				curr = new CGFactor(globid);
				curr.record(outcome);
				recorded = true;
				currentMap.put(globid, curr);
			}
		}
		// This apparent duplication is caused by a need to synchronize the "record" operation when the object is created
		// but a desire to have as little contention as possible.
		if (!recorded)
			curr.record(outcome);
		synchronized (workToDo) {
			workToDo.add(curr);
			workToDo.notify();
		}
	}

	public CGFactor get(String globid) {
		CGFactor ret;
		synchronized (currentMap) {
			ret = currentMap.get(globid);
			if (ret == null)
				return null;
		}
		return ret;
	}
}
