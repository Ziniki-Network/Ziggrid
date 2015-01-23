package org.ziggrid.driver;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.kvstore.foundation.ZiggridFTx;
import org.zinutils.exceptions.UtilException;

import com.foundationdb.Database;
import com.foundationdb.FDB;
import com.foundationdb.FDBException;
import com.foundationdb.KeyValue;
import com.foundationdb.NetworkOptions;
import com.foundationdb.Transaction;
import com.foundationdb.async.AsyncIterable;
import com.foundationdb.async.Function;

public class FoundationServerStore implements ServerStore {
	public class UpdateActiveThread extends Thread {
		public UpdateActiveThread() {
			this.setName("UpdateActive");
			this.setDaemon(true);
		}
		
		@Override
		public void run() {
			while (true) {
				try {
					db.run(new Function<Transaction, Void>() {
						@Override
						public Void apply(Transaction tx) {
							for (String key : myActives) {
								tx.set((key+".active").getBytes(), ZiggridFTx.mapbytes(new Date().getTime()));
							}
							return null;
						}
					});
					Thread.sleep(3500);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	public static Logger logger = LoggerFactory.getLogger("FoundationStore");
	private final FDB fdb;
	private final Database db;
	private final Set<String> myActives = new TreeSet<String>();
	private final UpdateActiveThread updateActive = new UpdateActiveThread(); 

	public FoundationServerStore(String clusterFile) {
		fdb = FDB.selectAPIVersion(200);
		NetworkOptions options = fdb.options();
		options.setTraceEnable("/tmp/fdb");
		try {
			if (clusterFile != null && clusterFile.length() == 0)
				clusterFile = null;
			logger.info("Attempting to open FoundationDB using " + (clusterFile != null?"cluster file " + clusterFile:"default cluster file"));
			db = fdb.open(clusterFile);
			logger.info("Opened FoundationDB");
		} catch (FDBException ex) {
			throw new UtilException("Could not open database with cluster file: " + clusterFile);
		}
	}

	@Override
	public void register(final String type, final String endpoint) {
		logger.info("Registering " + type + " " + endpoint);
		db.run(new Function<Transaction, Void>() {
			@Override
			public Void apply(Transaction tx) {
				String key = "server/" + type+ "/"+ endpoint;
				myActives.add(key);
				tx.set((key+".active").getBytes(), ZiggridFTx.mapbytes(new Date().getTime()));
				tx.set((key+".addr").getBytes(), ZiggridFTx.mapbytes(endpoint));
				tx.set((key+".created").getBytes(), ZiggridFTx.mapbytes(new Date().getTime()));
				tx.set((key+".type").getBytes(), ZiggridFTx.mapbytes(type));
				return null;
			}
		});
		if (!updateActive.isAlive())
			updateActive.start();
	}

	@Override
	public void delete(final ServerInfo s) {
		db.run(new Function<Transaction, Void>() {
			@Override
			public Void apply(Transaction tx) {
				byte[] from = ("server/" + s.key+".").getBytes();
				tx.clear(from, ZiggridFTx.fullRange(from));
				return null;
			}
		});
	}

	@Override
	public List<ServerInfo> list() {
		return db.run(new Function<Transaction, List<ServerInfo>>() {
			@Override
			public List<ServerInfo> apply(Transaction tx) {
				final List<ServerInfo> ret = new ArrayList<ServerInfo>();
				byte[] from = "server/".getBytes();
				AsyncIterable<KeyValue> range = tx.getRange(from, ZiggridFTx.fullRange(from));
				TreeMap<String, Object> map = new TreeMap<String, Object>();
				for (KeyValue kv : range) {
					String key = new String(kv.getKey());
					int idx = key.lastIndexOf(".");
					String field = key.substring(idx+1);
					map.put(field, ZiggridFTx.unmap(kv.getValue()));
					if (field.equals("type")) {
						ret.add(new ServerInfo((String)map.get("type"), (String)map.get("addr"), new Date((Long) map.get("created")), new Date((Long) map.get("active"))));
						map.clear();
					}
				}
				return ret;
			}
		});
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}
}
