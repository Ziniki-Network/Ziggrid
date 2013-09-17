package org.ziggrid.driver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.model.CorrelationDefinition;
import org.ziggrid.model.Grouping;
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

public class CorrelationViewProcessor implements ViewProcessor {
	private static final Logger logger = LoggerFactory.getLogger("CorrelationProcessor");
	private MaterializeObjects materializer;
	private CorrelationDefinition cd;
	private Grouping grouping;
	private CouchQuery globalQuery;
	private CouchQuery corrQuery;
	private final Set<List<Object>> allKeys = new LinkedHashSet<List<Object>>();
	private boolean bumpRequested;
	private static final Timer correlationProcessorTimer = CodeHaleMetrics.metrics.timer("CorrelationProcessorTimer");
	private static final Meter correlationProcessorMeter = CodeHaleMetrics.metrics.meter("CorrelationProcessorMeter");

	public CorrelationViewProcessor(CouchQuery globalQuery, CouchQuery corrQuery, MaterializeObjects materializer, CorrelationDefinition cd, Grouping grp) {
		this.globalQuery = globalQuery;
		this.corrQuery = corrQuery;
		this.materializer = materializer;
		this.cd = cd;
		grouping = grp;
		createMetrics();
	}


	@Override
	public Object keyFor(String key, JSONObject obj) {
		logger.debug("Correlation obtaining key for " + key + " from " + obj);
		List<Object> ret = new ArrayList<Object>();
		for (String f : grouping.fields){
			if (!obj.has(f))
				return null;
			try {
				ret.add(obj.get(f));
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
				corrQuery.bump();
				globalQuery.bump();
				bumpRequested = false;
			}
		}
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

	public void process(Set<List<Object>> keys) {
		final Timer.Context correlationProcessorContext = correlationProcessorTimer.time();
		logger.debug("Correlation processing " + keys);

		// First grab the global coefficient entries
		Map<String, double[]> global = new HashMap<String, double[]>();
		{
			Query q = new Query();
			q.setReduce(true);
			q.setStale(Stale.OK);
			q.setGroupLevel(cd.items.size());
			ViewResponse resp = globalQuery.query(q);
			for (ViewRow r : resp) {
				try {
					JSONObject v = new JSONObject(r.getValue());
					global.put(r.getKey(), new double[] { v.getDouble("sum"), v.getDouble("count") });
				} catch (JSONException ex) {
					logger.error(ex.getMessage());
				}
			}
			logger.debug("Correlation global map has " + global.size() + " keys");
		}

		for (List<Object> ks : keys) {
			Query q = new Query();
			q.setStale(Stale.OK);
			q.setReduce(true);
			q.setGroupLevel(grouping.fields.size() + cd.items.size());

			JSONArray start = new JSONArray();
			JSONArray end = new JSONArray();
			for (Object k : ks) {
				start.put(k);
				end.put(k);
			}
			end.put("\uefff");
			q.setRange(start.toString(), end.toString());

			ViewResponse resp = corrQuery.query(q);
			try {
				Map<String, double[]> me = new HashMap<String, double[]>();
				String meKey = start.toString();
				for (ViewRow r : resp) {
					JSONObject stats = new JSONObject(r.getValue());
					me.put("["+r.getKey().substring(meKey.length()), new double[] { stats.getDouble("sum"), stats.getDouble("count") });
				}
				materializer.materializeCorrelationObject(cd, corrQuery.getViewName(), global, grouping, meKey, me);
			} catch (JSONException ex) {
				logger.error(ex.getMessage());
			}
		}
		correlationProcessorContext.stop();
		correlationProcessorMeter.mark(keys.size());
	}

	@Override
	public void bump() {
		bumpRequested = true;
	}
	
	@Override
	public String toString() {
		return "View Correlating " + cd.name + " from " + cd.from;
	}

	public String toThreadName() {
		return "Corr-" + cd.name + "-" + cd.from + grouping.asGroupName();
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
