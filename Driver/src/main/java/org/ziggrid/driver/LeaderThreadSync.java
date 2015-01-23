package org.ziggrid.driver;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.ziggrid.api.StoreableObject;
import org.ziggrid.api.TransmittableObject;
import org.ziggrid.api.TransmittableObject.Table;
import org.ziggrid.driver.interests.InterestEngine;
import org.ziggrid.kvstore.KVStorageEngine;
import org.ziggrid.kvstore.KVTransaction;
import org.ziggrid.model.Grouping;
import org.zinutils.sync.SyncUtils;

import com.foundationdb.FDBException;

public class LeaderThreadSync {
	public class LTRecord {
		private final String group;
		private final String value;
		private String sorts;
		private final String oldSorts;

		public LTRecord(String group, String value, String sorts, String oldSorts) {
			this.group = group;
			this.value = value;
			this.sorts = sorts;
			this.oldSorts = oldSorts;
		}

		public String queueKey() {
			return group+"/"+value;
		}

		public void updateWith(LTRecord r) {
			this.sorts = r.sorts;
		}
	}
	
	public class LTThread extends Thread {
		private Map<String, Map<String, String>> currentBoards = new HashMap<String, Map<String, String>>();

		public void run() {
			int delay = 10;
			Date nextCheckBoard = new Date(new Date().getTime()-100); // always get it very early on
			int checkBoardLag = 100;
			while (true) {
				try {
					Entry<String, LTRecord> curr = null;
					synchronized (queue) {
						if (queue.isEmpty()) {
							SyncUtils.waitFor(queue, delay);
							delay = Math.min(delay*2, 5000);
						} else {
							delay = 10;
							curr = queue.entrySet().iterator().next();
							queue.remove(curr.getKey());
						}
					}
					if (curr != null) {
	//					System.out.println("processing " + curr.getKey());
						boolean willCommit = false;
						KVTransaction tx = null;
						try {
							tx = store.database().beginTx();
							String value = curr.getValue().value;
							String group = curr.getValue().group;
							String sorts = curr.getValue().sorts;
							String oldSorts = curr.getValue().oldSorts;
							String lid = "leaderboard/"+sha+group+sorts+"/"+value;
							String olid = oldSorts != null ? "leaderboard/" + sha + group + oldSorts+"/"+value : null;
					
							tx.put(lid, value);
							if (olid != null)
								tx.deleteKey(olid);
							
							willCommit = true;
							tx.commit();
						} catch (FDBException ex) {
							ActingLeader.logger.error("Conflict occurred trying to write " + curr.getKey());
							if (!willCommit && tx != null)
								tx.rollback();
							// add it back to the end of the queue
							synchronized (queue) {
								queue.put(curr.getKey(), curr.getValue());
							}
						}
					}
				} catch (Throwable t) {
					t.printStackTrace();
				}
				Date now = new Date();
				if (now.after(nextCheckBoard)) {
					// read board
					List<String> iterateThrough;
					synchronized (knownGroups) {
						iterateThrough = new ArrayList<String>(knownGroups);
					}
					synchronized (currentBoards) {
						for (String grp : iterateThrough) {
							boolean willCommit = false;
							KVTransaction tx = null;
							try {
								tx = store.database().beginTx();
								Map<String, String> lb = tx.getKeyRangePrefixed("leaderboard/"+sha+grp, top, wantAscending);
								boolean updated = false;
								if (!currentBoards.containsKey(grp)) {
									updated = true;
									tx.put("leaderboardg/"+sha+grp, grp);
								} else {
									Map<String, String> prev = currentBoards.get(grp);
									if (lb.size() != prev.size())
										updated = true;
									else {
										Iterator<Entry<String, String>> pi = prev.entrySet().iterator();
										for (Entry<String, String> e : lb.entrySet()) {
											Entry<String, String> next = pi.next();
											if (!e.getKey().equals(next.getKey()) || !e.getValue().equals(next.getValue())) {
												updated = true;
												break;
											}
										}
									}
								}
								if (updated) {
									currentBoards.put(grp, lb);
									List<StoreableObject> updatedObjects = new ArrayList<StoreableObject>();
									updatedObjects.add(getTransmittableTable(grp, lb));
									interests.process(updatedObjects);
								}
								willCommit = true;
								tx.commit();
							} catch (FDBException ex) {
								ActingLeader.logger.error("Conflict occurred trying to read leaderboard");
								if (!willCommit && tx != null)
									tx.rollback();
							}
						}
					}
					// see if it's different
					// if not, double delay
					checkBoardLag = Math.min(checkBoardLag*2, 2500);
					// wait for next
					nextCheckBoard = new Date(now.getTime()+checkBoardLag);
				}
				/*
							// all this belongs elsewhere in a different tx thread
						Transaction ftx = Reflection.getField(tx, "tx");
						String p1 = "leaderboard/"+sha+group;
						AsyncIterable<KeyValue> range = ftx.getRange(p1.getBytes(), (p1+"\uffff").getBytes(), 10, true);
						System.out.println("-----");
						for (KeyValue kv : range)
							System.out.println(new String(kv.getKey()));
						System.out.println("-----");
				 */
			}
		}

		public TransmittableObject getTransmittableTable(String grp, Map<String, String> lb) {
			TransmittableObject lbObj = new TransmittableObject(watchName, grp);
			String[] fields = grp.split("/");
			int k = 1;
			for (String g : grpDefn.fields)
				lbObj.set(g, fields[k++]);
			Table table = lbObj.table("table");
			for (Entry<String, String> e : lb.entrySet()) {
				String value = e.getValue();
				String sorts = e.getKey().replace("leaderboard/", "").replace(sha, "").replace(grp, "");
				sorts = sorts.substring(1, sorts.length()-value.length()-1);
//									System.out.println(sorts + "=>" + value);
				Object fvalue;
				if (sorts.contains("."))
					fvalue = Double.parseDouble(sorts);
				else
					fvalue = Integer.parseInt(sorts);
				table.add(fvalue, value);
			}
			return lbObj;
		}
	}

	private final KVStorageEngine store;
	private final InterestEngine interests;
	private final String watchName;
	private final Grouping grpDefn;
	private final String sha;
	private final int top;
	private final boolean wantAscending;
	private final LinkedHashMap<String, LTRecord> queue = new LinkedHashMap<String, LTRecord>();
	private final Set<String> knownGroups = new TreeSet<String>();
	private final LTThread workProcessor;

	public LeaderThreadSync(KVStorageEngine store, InterestEngine interests, String watchName, Grouping grp, String sha, int top, boolean wantAscending) {
		this.store = store;
		this.interests = interests;
		this.watchName = watchName;
		this.grpDefn = grp;
		this.sha = sha;
		this.top = top;
		this.wantAscending = wantAscending;
		readExistingGroups();
		workProcessor = new LTThread();
		workProcessor.setName("LTProcessor-"+watchName);
		workProcessor.setDaemon(true);
		workProcessor.start();
	}

	private void readExistingGroups() {
		KVTransaction tx = null;
		boolean willCommit = false;
		try {
			tx = store.database().beginTx();
			Map<String, String> groups = tx.getKeyRangePrefixed("leaderboardg/" + sha + "/", -1, true);
			knownGroups.addAll(groups.values());
			willCommit = true;
			tx.commit();
		} catch (Throwable t) {
			t.printStackTrace();
			System.out.println("Yo!");
		} finally {
			if (!willCommit && tx != null)
				tx.rollback();
		}
	}

	public void update(String group, String value, String sorts, String oldSorts) {
		if (oldSorts != null && oldSorts.equals(sorts))
			return;
		LTRecord r = new LTRecord(group, value, sorts, oldSorts);
		String key = r.queueKey();
		synchronized (queue) {
			if (queue.containsKey(key))
				queue.get(key).updateWith(r);
			else
				queue.put(key, r);
			queue.notify();
		}
		synchronized (knownGroups) {
			knownGroups.add(group);
		}
	}

	public StoreableObject canYouProvide(Object inTx, String watchable, Map<String, Object> options) {
		try {
			StringBuilder sb = new StringBuilder();
			for (String s : grpDefn.fields) {
				if (!options.containsKey(s))
					return null;
				sb.append("/");
				sb.append(options.get(s));
			}
			String key = sb.toString();
			synchronized (workProcessor.currentBoards) {
				if (workProcessor.currentBoards.containsKey(key))
					return workProcessor.getTransmittableTable(key, workProcessor.currentBoards.get(key));
			}
			return null;
		} catch (Exception ex) {
			return null;
		}
	}
}
