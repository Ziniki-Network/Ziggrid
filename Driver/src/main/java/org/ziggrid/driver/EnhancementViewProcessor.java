package org.ziggrid.driver;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.model.EnhancementDefinition;
import org.ziggrid.utils.metrics.CodeHaleMetrics;
import org.ziggrid.utils.sync.SyncUtils;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.couchbase.client.protocol.views.Query;
import com.couchbase.client.protocol.views.Stale;
import com.couchbase.client.protocol.views.ViewResponse;
import com.couchbase.client.protocol.views.ViewRow;

public class EnhancementViewProcessor implements ViewProcessor {
	private static final Logger logger = LoggerFactory.getLogger("EnhancementProcessor");
	private final CouchQuery query;
	private final MaterializeObjects materializer;
	private final EnhancementDefinition defn;
	private static final Timer enhancementProcessorTimer = CodeHaleMetrics.metrics.timer("EnhancementProcessorTimer");
	private static final Meter enhancementProcessorMeter = CodeHaleMetrics.metrics.meter("EnhancementProcessorMeter");
	private final Set<String> allKeys = new LinkedHashSet<String>();
	private boolean bumpRequested;
	
	public EnhancementViewProcessor(CouchQuery query, MaterializeObjects materializer, EnhancementDefinition defn) {
		this.query = query;
		this.materializer = materializer;
		this.defn = defn;
		createMetrics();
	}

	@Override
	public Object keyFor(String key, JSONObject obj) {
		return key;
	}

	
	@Override
	public void spill(Set<Object> keys) {
		synchronized (allKeys) {
			if (!allKeys.isEmpty())
				logger.error(this + ".spill() called with " + keys.size() + " new entries, when there are still " + allKeys.size() + " waiting to be processed");
			for (Object o : keys)
				allKeys.add((String)o);
			allKeys.notify();
		}
	}

	@Override
	public void run() {
		while (true) {
			JSONArray arr;
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
	
	public JSONArray gather(int size) {
		int pos = 0;
		JSONArray arr = new JSONArray();
		Iterator<String> it = allKeys.iterator();
		while (it.hasNext()) {
			arr.put(defn.keyName(it.next()));
			it.remove();
			if (pos++ >= size)
				break;
		}
		return arr;
	}
	
	private void process(JSONArray arr) {
		final Timer.Context enhancementProcessorContext = enhancementProcessorTimer.time();
		Query q = new Query();
		q.setKeys(arr.toString());
		q.setStale(Stale.OK);
		logger.debug("Reading keys " + arr);
		ViewResponse response = query.query(q);
		for (ViewRow row : response) {
			materializer.materialize(logger, defn.to, row.getKey(), null, null, defn.fieldNames, row.getValue());
		}
		enhancementProcessorContext.stop();
		enhancementProcessorMeter.mark(arr.length());
	}

	@Override
	public void bump() {
		bumpRequested = true;
	}
	
	@Override
	public String toString() {
		return "Enhancing " + defn.from + " to " + defn.to;
	}

	public String toThreadName() {
		return "Enh-" + defn.from + "-" + defn.to;
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

