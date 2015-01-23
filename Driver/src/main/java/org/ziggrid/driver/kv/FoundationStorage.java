package org.ziggrid.driver.kv;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.api.AnalyticItem;
import org.ziggrid.api.ExistingObjectProvider;
import org.ziggrid.api.IInterestEngine;
import org.ziggrid.api.IModel;
import org.ziggrid.api.StoreableObject;
import org.ziggrid.api.TickUpdate;
import org.ziggrid.config.FoundationStorageConfig;
import org.ziggrid.config.StorageConfig;
import org.ziggrid.driver.MappingEnhancer;
import org.ziggrid.driver.interests.InterestEngine;
import org.ziggrid.kvstore.KVDatabase;
import org.ziggrid.kvstore.KVStorageEngine;
import org.ziggrid.kvstore.KVStore;
import org.ziggrid.kvstore.KVTransaction;
import org.ziggrid.kvstore.foundation.FoundationDatabase;
import org.ziggrid.kvstore.foundation.ZiggridFTx;
import org.zinutils.collections.ListMap;
import org.zinutils.exceptions.UtilException;
import org.zinutils.metrics.CodaHaleMetrics;
import org.zinutils.sync.SyncUtils;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.foundationdb.Database;
import com.foundationdb.FDB;
import com.foundationdb.FDBException;
import com.foundationdb.KeyValue;
import com.foundationdb.NetworkOptions;
import com.foundationdb.Transaction;
import com.foundationdb.async.AsyncIterable;
import com.foundationdb.async.Function;

public class FoundationStorage implements KVStorageEngine {
	public static Logger logger = LoggerFactory.getLogger("FoundStorage");
	private FDB fdb;
	private Database db;
	private MappingEnhancer mapper;
	private FoundationDatabase kvdb;
	private InterestEngine interests;
	private boolean shouldDump;
	private static Timer insideTimer = CodaHaleMetrics.metrics.timer(MetricRegistry.name("generatorInsideTxTimer"));
	private static Timer outsideTimer = CodaHaleMetrics.metrics.timer(MetricRegistry.name("generatorTxTimer"));
	private static Timer writeTimer = CodaHaleMetrics.metrics.timer(MetricRegistry.name("generatorWriteTimer"));
	private short unique;

	@Override
	public void open(IInterestEngine interests, IModel model, StorageConfig storage) {
		this.interests = (InterestEngine) interests;
		FoundationStorageConfig fsc = (FoundationStorageConfig) storage;
		mapper = new MappingEnhancer(model);
		fdb = FDB.selectAPIVersion(200);
		NetworkOptions options = fdb.options();
		options.setTraceEnable("/tmp/fdb");
		try {
			db = fdb.open(fsc.clusterFile());
			unique = db.run(new Function<Transaction, Short>() {
				public Short apply(Transaction tx) {
					byte[] key = "fdb.nextUnique".getBytes();
					byte[] val = tx.get(key).get();
					short me;
					if (val == null)
						me = 1;
					else
						me = (short)(Short.parseShort(new String(val))+1);
					tx.set(key, Integer.toString(me).getBytes());
					return me;
				}
			});
			kvdb = new FoundationDatabase(db, unique);
		} catch (FDBException ex) {
			throw new UtilException("Could not open database with cluster file: " + fsc.clusterFile());
		}
		if (fsc.clearDB()) {
			logger.warn("Clearing database");
			clearDB();
		}
		if (fsc.shouldDump()) {
			shouldDump = true;
			dump();
		}
	}

	@Override
	public short unique() {
		return unique;
	}

	@Override
	public KVDatabase database() {
		return kvdb;
	}

	public void clearDB() {
		db.run(new Function<Transaction,Void>() {
			@Override
			public Void apply(Transaction tr) {
				tr.clear(new byte[] { 0x00 }, new byte[] { (byte) 0xff });
				return null;
			}
		});
	}
	
	
	@Override
	public StoreableObject findExisting(ListMap<String, ? extends ExistingObjectProvider> processors, String tlc, Map<String, Object> options) {
		ZiggridFTx tx = kvdb.beginTx();
		try {
			if (processors.contains(tlc)) {
				List<? extends ExistingObjectProvider> generators = processors.get(tlc);
				for (ExistingObjectProvider g : generators) {
					StoreableObject so = g.canYouProvide(tx, tlc, options);
					if (so != null)
						return so;
				}
			}
		} finally {
			tx.commit();
		}
		return null;
	}

	@Override
	public void recordServer(final String id, final JSONObject obj) {
		throw new UtilException("This appears to be dead code; register is used instead");
//		db.run(new Function<Transaction,Void>() {
//			@Override
//			public Void apply(Transaction tx) {
//				FoundationTx ftx = kvdb.wrap(tx);
//				@SuppressWarnings("unchecked")
//				Iterator<Object> it = obj.keys();
//				while (it.hasNext()) {
//					try {
//						String k = (String) it.next();
//						ftx.setField("s/" + id, k, obj.get(k));
//					} catch (Exception ex) {
//						logger.error("Error encountered writing server", ex);
//					}
//				}
//				kvdb.unwrap(ftx);
//				return null;
//			}
//		});
	}

	@Override
	public void syncTo(final int id, final int currentPosition) {
		int gap = 5;
		if (currentPosition%gap != 0)
			return;
		int delay = 100;
		Context syncContext = CodaHaleMetrics.metrics.timer("SyncTimer-" + id).time();
		doCheck:
		while (true) {
			List<Integer> curr = db.run(new Function<Transaction,List<Integer>>() {
				public List<Integer> apply(Transaction tx) {
					tx.set(("genSync/"+id).getBytes(), Integer.toString(currentPosition).getBytes());
					List<Integer> all = new ArrayList<Integer>();
					for (KeyValue kv : tx.getRange("genSync/".getBytes(), "genSync?".getBytes())) {
						all.add(Integer.parseInt(new String(kv.getValue())));
					}
					return all;
				}
			});
			
			for (int p : curr) {
				if (currentPosition > p+gap) {
					logger.info("Waiting for sluggards; I'm at " + currentPosition + " but overall = " + curr + "; waiting for " + p);
					SyncUtils.sleep(delay);
					delay = Math.min(delay*2, 1000);
					continue doCheck;
				}
			}
			break;
		}
		syncContext.stop();
	}

	@Override
	public boolean push(final TickUpdate toSave) {
		Context outsideContext = outsideTimer.time();
		try {
			Function<ZiggridFTx,Collection<StoreableObject>> processTx = new Function<ZiggridFTx,Collection<StoreableObject>>() {
				@Override
				public Collection<StoreableObject> apply(ZiggridFTx ftx) {
					Context insideContext = insideTimer.time();
					try {
						KVStore store = getStoreForTx(ftx); 
						for (StoreableObject i : toSave.updates.values()) {
							Context writer = writeTimer.time();
							store.write(i);
							writer.stop();
						}
						for (AnalyticItem ai : toSave.items) {
							Context writer = writeTimer.time();
							store.store(ai);
							writer.stop();
						}
						kvdb.unwrap(ftx);
						return ftx.updatedObjects();
					} catch (FDBException ex) {
						logger.error("Transaction " + ftx + " failed during push: " + ex.toString());
						throw ex;
					} finally {
						insideContext.stop();
					}
				}
			};
			Collection<StoreableObject> updates;
			while (true) {
				ZiggridFTx tx = kvdb.beginTx();
				try {
					updates = processTx.apply(tx);
					logger.debug("Committing tx");
					tx.commit();
					break;
				} catch (FDBException ex) {
					logger.error("Transaction " + tx + " failed for generator: " + ex.toString() + " ... retrying");
				} finally {
					kvdb.cleanupTx(tx);
				}
			}
			interests.process(updates);
			return true;
		} finally {
			outsideContext.stop();
		}
	}

	@Override
	public void close() {
		if (db != null) {
			if (shouldDump)
				dump();
			db.dispose();
			kvdb = null;
		}
	}

	public void dump() {
		System.out.println("-----");
		db.run(new Function<Transaction,Void>() {
			@Override
			public Void apply(Transaction tr) {
				byte[] from = "".getBytes();
				AsyncIterable<KeyValue> range = tr.getRange(from, ZiggridFTx.fullRange(from));
				String prevKey = null;
				Map<String, Object> obj = new TreeMap<String, Object>();
				for (KeyValue kv : range) {
					String key = new String(kv.getKey());
					try {
						int idx = key.indexOf('.');
						if (idx == -1 || key.startsWith("server/")) {
							if (prevKey != null) {
								System.out.println(prevKey + " => " + obj);
								prevKey = null;
								obj = new TreeMap<String, Object>();
							}
							System.out.println(key + "=>" + new String(kv.getValue()));
						} else {
							String newKey = key.substring(0, idx);
							String field = key.substring(idx+1);
							if (prevKey != null && !newKey.equals(prevKey)) {
								System.out.println(prevKey + " => " + obj);
								obj = new TreeMap<String, Object>();
							}
							prevKey = newKey;
							obj.put(field, ZiggridFTx.unmap(kv.getValue()));
						}
					} catch (Exception ex) {
						logger.error("Could not render key " + key, ex);
						throw UtilException.wrap(ex);
					}
				}
				if (prevKey != null)
					System.out.println(prevKey + " => " + obj);
				return null;
			}
		});
		System.out.println("-----");
	}

	@Override
	public KVStore getStoreForTx(KVTransaction tr) {
		return new KVStore(this, mapper, tr);
	}

	@Override
	public boolean has(final String gameId) {
		int cnt = 0;
		FDBException tmpEx = null;
		while (cnt++ < 5) {
			ZiggridFTx tx = kvdb.beginTx();
			try {
				boolean ret = tx.contains(gameId);
				logger.debug("Committing tx");
				tx.commit();
				return ret;
			} catch (FDBException ex) {
				logger.error("Transaction " + tx + " failed for generator: " + ex.toString() + " ... retrying");
				tmpEx = ex;
			} finally {
				kvdb.cleanupTx(tx);
			}
		}
		throw tmpEx;
	}
}
