package org.ziggrid.driver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.spy.memcached.CASResponse;
import net.spy.memcached.CASValue;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.exceptions.ZiggridException;
import org.ziggrid.model.Enhancement;
import org.ziggrid.model.FieldEnhancement;
import org.ziggrid.model.SnapshotDefinition;
import org.ziggrid.utils.collections.ListMap;
import org.ziggrid.utils.metrics.CodeHaleMetrics;
import org.ziggrid.utils.sync.SyncUtils;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.couchbase.client.CouchbaseClient;

public class SnapshotLocalProcessor implements LocalProcessor {
	private static final Logger logger = LoggerFactory.getLogger("SnapshotProcessor");
	private final MaterializeObjects materializer;
	private final SnapshotDefinition sd;
	private final List<String> keyFields = new ArrayList<String>();
	private final ListMap<List<Object>, JSONObject> allEntries = new ListMap<List<Object>, JSONObject>();
	private final LinkedHashSet<List<Object>> linkedKeys = new LinkedHashSet<List<Object>>();
	private final EnhancementVM enhancer = new EnhancementVM();
	private final CouchbaseClient conn;
	private static final Timer snapshotProcessorTimer = CodeHaleMetrics.metrics.timer("SnapshotProcessorTimer");
	private static final Meter snapshotProcessorMeter = CodeHaleMetrics.metrics.meter("SnapshotProcessorMeter");
	private final String upTo;

	public SnapshotLocalProcessor(CouchbaseClient conn, MaterializeObjects materializer, SnapshotDefinition sd) {
		this.conn = conn;
		this.materializer = materializer;
		this.sd = sd;
		for (Enhancement expr : sd.group){
			if (!(expr instanceof FieldEnhancement))
				throw new ZiggridException("That is not yet supported");
			keyFields.add(((FieldEnhancement)expr).field);
		}
		if (!(sd.upTo instanceof FieldEnhancement))
			throw new ZiggridException("That is not yet supported");
		upTo = ((FieldEnhancement)sd.upTo).field;
		keyFields.add(upTo);
		createMetrics();
	}
	
	@Override
	public Object keyFor(String key, JSONObject obj) {
		logger.debug("Snapshot obtaining key for " + key + " from " + obj);
		List<Object> ret = new ArrayList<Object>();
		for (String field : keyFields){
			if (!obj.has(field))
				return null;
			try {
				ret.add(obj.get(field));
			} catch (JSONException ex) {
				ex.printStackTrace();
				return null;
			}
		}
		logger.debug("Returning key " + ret);
		return ret;
	}

	@Override
	public void run() {
		while (true) {
			List<Object> key;
			List<JSONObject> entries;
			synchronized (allEntries) {
				while (allEntries.isEmpty()) {
					SyncUtils.waitFor(allEntries, 0);
				}
				key = linkedKeys.iterator().next();
				linkedKeys.remove(key);
				entries = allEntries.removeAll(key);
			}
			process(key, entries);
		}
	}

	@Override
	public void spill(Set<Object> keys) {
		throw new ZiggridException("Cannot use views");
	}
	
	@Override
	public void spill(ListMap<Object, JSONObject> keyedEntries) {
		synchronized (allEntries) {
			if (!allEntries.isEmpty()) {
				logger.error(this + ".spill() called with " + keyedEntries.totalSize() + " new entries, when there are still " + allEntries.totalSize() + " waiting to be processed");
			}
			for (Object k1 : keyedEntries.keySet()) {
				@SuppressWarnings("unchecked")
				List<Object> k = (List<Object>) k1;
				linkedKeys.add(k);
				for (JSONObject o : keyedEntries.get(k1))
					allEntries.add(k, o);
			}
			allEntries.notify();
		}
	}

	private void process(List<Object> key, List<JSONObject> entries) {
		final Timer.Context snapshotProcessorContext = snapshotProcessorTimer.time();
		logger.debug("Snapshot processing " + key);
		
		JSONArray jk = new JSONArray();
		for (int i=0;i<key.size()-1;i++)
			jk.put(key.get(i));
		String jks = jk.toString();
		jk.put(key.get(key.size()-1));

		String id = materializer.computeSHAId(sd.getViewName()+"-factors", jks);
		boolean processed = false;
		while (!processed) {
			CASValue<Object> cas = conn.gets(id);
			final JSONObject factors;
			
			try {
				if (cas == null) {
					// recover factors from disk
					factors = new JSONObject();
				} else
					factors = new JSONObject((String) cas.getValue());

				// Update factors
				Set<Integer> recomputeFor = new HashSet<Integer>();
				for (JSONObject o : entries) {
					int line = o.getInt(upTo);
					recomputeFor.add(line);
					JSONObject factor = new JSONObject();
					for (int x = 0; x<sd.valueFields.size();x++) {
						factor.put(sd.valueFields.get(x), enhancer.process(sd.values.get(x), o));
					}
					factors.put(Integer.toString(line), factor);
				}
				
				// Recalculate affected items
//				logger.info("Recomputing for key " + jk.toString() + " with rcf = " + recomputeFor);
				for (int endAt : recomputeFor) {
					materializer.materializeSnapshotObject(sd, keyFields, sd.valueFields, (int) sd.startFrom(endAt), endAt, sd.getViewName(), jk.toString(), factors);
				}
				
				// Save to Couchbase
				logger.debug("Saving snapshot factors " + id);
				if (cas == null) {
					Boolean r = conn.add(id, 0, factors.toString()).get();
					if (r)
						processed = true;
					else
						logger.error("Add failed for " + jks + "; trying read again");
				} else {
					CASResponse r = conn.cas(id, cas.getCas(), factors.toString());
					if (r == CASResponse.OK)
						processed = true;
					else
						logger.error("CAS Failed for " + jks + "("+r+"); trying again");
				}
			} catch (Exception ex) {
				logger.error("Something bad went wrong, claiming we're done, but results will be inaccurate", ex);
				processed = true; // lies
			}

		}
		
		/*
			Query q = new Query();
			q.setStale(Stale.OK);
			q.setReduce(false);

			JSONArray start = new JSONArray();
			JSONArray end = new JSONArray();
			for (Object k : key) {
				start.put(k);
				end.put(k);
			}
			try {
				int endAt = (Integer) key.get(key.size()-1);
				start.put(start.length()-1, sd.startFrom(endAt));
				q.setRange(start.toString(), end.toString());
				
//				ViewResponse resp = query.query(q);
//				materializer.materializeSnapshotObject(sd, keyFields, sd.valueFields, endAt, sd.getViewName(), end.toString(), resp);
			} catch (JSONException ex) {
				ex.printStackTrace();
			}
			*/
		snapshotProcessorContext.stop();
		snapshotProcessorMeter.mark(1);
	}

	@Override
	public void bump() {
	}

	@Override
	public String toString() {
		return "Snapshot of " + sd.from;
	}

	public String toThreadName() {
		return "Snap-" + sd.from;
	}
	
	private void createMetrics() {
		CodeHaleMetrics.metrics.register(MetricRegistry.name(this.toThreadName() + "-EntryGauge"),
				new Gauge<Integer>() {
			@Override
			public Integer getValue() {
				synchronized (allEntries) {
					return allEntries.totalSize();
				}
			}
		});
	}
}
