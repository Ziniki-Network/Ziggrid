package org.ziggrid.driver.kv;

import java.util.ArrayList;
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
import org.ziggrid.config.StorageConfig;
import org.ziggrid.driver.MappingEnhancer;
import org.ziggrid.driver.interests.InterestEngine;
import org.ziggrid.kvstore.KVStorageEngine;
import org.ziggrid.kvstore.KVStore;
import org.ziggrid.kvstore.KVTransaction;
import org.ziggrid.kvstore.memory.MemoryDatabase;
import org.ziggrid.kvstore.memory.MemoryTx;
import org.zinutils.collections.ListMap;
import org.zinutils.metrics.CodaHaleMetrics;
import org.zinutils.sync.SyncUtils;

import com.codahale.metrics.Timer.Context;
import com.foundationdb.async.Function;

public class MemoryStorage implements KVStorageEngine {
	public static Logger logger = LoggerFactory.getLogger("MemoryStorage");
	private MemoryDatabase db;
	private MappingEnhancer mapper;
	private final Map<Integer,Integer> currGeneratorPositions = new TreeMap<Integer,Integer>();
	private InterestEngine interests;

	@Override
	public void open(IInterestEngine engine, IModel model, StorageConfig storage) {
		this.interests = (InterestEngine) engine;
		db = new MemoryDatabase();
		mapper = new MappingEnhancer(model);
	}

	@Override
	public short unique() {
		return 0;
	}

	@Override
	public StoreableObject findExisting(ListMap<String, ? extends ExistingObjectProvider> processors, String tlc, Map<String, Object> options) {
		MemoryTx tx = db.beginTx();
		if (processors.contains(tlc)) {
			List<? extends ExistingObjectProvider> generators = processors.get(tlc);
			for (ExistingObjectProvider g : generators) {
				StoreableObject so = g.canYouProvide(tx, tlc, options);
				if (so != null)
					return so;
			}
		}
		return null;
	}

	@Override
	public void recordServer(String id, JSONObject obj) {
	}

	@Override
	public boolean push(final TickUpdate toSave) {
		final ArrayList<StoreableObject> updates = new ArrayList<StoreableObject>();
		db.run(new Function<KVTransaction,Void>() {
			@Override
			public Void apply(KVTransaction tr) {
				KVStore store = getStoreForTx(tr);
				for (StoreableObject i : toSave.updates.values()) {
					store.write(i);
					updates.add(i);
				}
				for (AnalyticItem ai : toSave.items) {
					store.store(ai);
					updates.add(ai);
				}
				return null;
			}
		});
		interests.process(updates);
		return true;
	}

	@Override
	public void close() {
//		db.dump();
		db.dispose();
	}

	public MemoryDatabase database() {
		return db;
	}

	@Override
	public KVStore getStoreForTx(KVTransaction tr) {
		return new KVStore(this, mapper, tr);
	}

	@Override
	public void syncTo(int id, int currentPosition) {
		int gap = 5;
		if (currentPosition%gap != 0)
			return;
		currGeneratorPositions.put(id, currentPosition);
		int delay = 100;
		Context syncContext = CodaHaleMetrics.metrics.timer("SyncTimer-" + id).time();
		doCheck:
		while (true) {
			for (Integer p : currGeneratorPositions.values()) {
				if (currentPosition > p+gap) {
					logger.info("Waiting for sluggards; I'm at " + currentPosition + " but overall = " + currGeneratorPositions + "; waiting for " + p);
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
	public boolean has(final String gameId) {
		return db.run(new Function<KVTransaction, Boolean>() {
			@Override
			public Boolean apply(KVTransaction tx) {
				return tx.contains(gameId);
			}
		});
	}
}
