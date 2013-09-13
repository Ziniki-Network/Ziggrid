package org.ziggrid.driver;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.exceptions.ZiggridException;
import org.ziggrid.model.Enhancement;
import org.ziggrid.model.FieldEnhancement;
import org.ziggrid.model.SnapshotDefinition;
import org.ziggrid.utils.metrics.CodeHaleMetrics;
import org.ziggrid.utils.sync.SyncUtils;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.couchbase.client.protocol.views.Query;
import com.couchbase.client.protocol.views.Stale;
import com.couchbase.client.protocol.views.ViewResponse;

public class SnapshotViewProcessor implements ViewProcessor {
	private static final Logger logger = LoggerFactory.getLogger("SnapshotProcessor");
	private final CouchQuery query;
	private final MaterializeObjects materializer;
	private final SnapshotDefinition sd;
	private final List<String> keyFields = new ArrayList<String>();
	private final Set<List<Object>> allKeys = new LinkedHashSet<List<Object>>();
	private boolean bumpRequested;
	private static final Timer snapshotProcessorTimer = CodeHaleMetrics.metrics.timer("SnapshotProcessorTimer");
	private static final Meter snapshotProcessorMeter = CodeHaleMetrics.metrics.meter("SnapshotProcessorMeter");

	public SnapshotViewProcessor(CouchQuery query, MaterializeObjects materializer, SnapshotDefinition sd) {
		this.query = query;
		this.materializer = materializer;
		this.sd = sd;
		for (Enhancement expr : sd.group){
			if (!(expr instanceof FieldEnhancement))
				throw new ZiggridException("That is not yet supported");
			keyFields.add(((FieldEnhancement)expr).field);
		}
		if (!(sd.upTo instanceof FieldEnhancement))
			throw new ZiggridException("That is not yet supported");
		keyFields.add(((FieldEnhancement)sd.upTo).field);
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
			Set<List<Object>> arr;
			synchronized (allKeys) {
				while (allKeys.isEmpty())
					SyncUtils.waitFor(allKeys, 0);
				arr = gather(Ziggrid.groupSize);
			}
			process(arr);
			
			if(bumpRequested) {
				query.bump();
				bumpRequested = false;
			}
		}
	}

	public Set<List<Object>> gather(int size) {
		int pos = 0;
		Set<List<Object>> arr = new LinkedHashSet<List<Object>>();
		Iterator<List<Object>> it = allKeys.iterator();
		while (it.hasNext()) {
			arr.add(it.next());
			it.remove();
			if (pos++ >= size)
				break;
		}
		return arr;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void spill(Set<Object> keys) {
		synchronized (allKeys) {
			if (!allKeys.isEmpty())
				logger.error(this + ".spill() called with " + keys.size() + " new entries, when there are still " + allKeys.size() + " waiting to be processed");
			for (Object o : keys)
				allKeys.add((List<Object>)o);
			allKeys.notify();
		}
			
	}

	public void process(Set<List<Object>> keys) {
		final Timer.Context snapshotProcessorContext = snapshotProcessorTimer.time();
		logger.debug("Snapshot processing " + keys);
		
		for (List<Object> ks : keys) {
			Query q = new Query();
			q.setStale(Stale.OK);
			q.setReduce(false);

			JSONArray start = new JSONArray();
			JSONArray end = new JSONArray();
			for (Object k : ks) {
				start.put(k);
				end.put(k);
			}
			try {
				int endAt = (Integer) ks.get(ks.size()-1);
				start.put(start.length()-1, sd.startFrom(endAt));
				q.setRange(start.toString(), end.toString());
				
				ViewResponse resp = query.query(q);
//				materializer.materializeSnapshotObject(sd, keyFields, sd.valueFields, endAt, query.getViewName(), end.toString(), resp);
			} catch (JSONException ex) {
				ex.printStackTrace();
			}
		}
		snapshotProcessorContext.stop();
		snapshotProcessorMeter.mark(keys.size());
	}

	@Override
	public void bump() {
		bumpRequested = true;
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
				synchronized (allKeys) {
					return allKeys.size();
				}
			}
		});
	}
}
