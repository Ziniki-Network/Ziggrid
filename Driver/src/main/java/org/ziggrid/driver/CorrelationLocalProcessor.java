package org.ziggrid.driver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.spy.memcached.CASResponse;
import net.spy.memcached.CASValue;
import net.spy.memcached.internal.GetFuture;
import net.spy.memcached.internal.OperationFuture;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.exceptions.ZiggridException;
import org.ziggrid.model.CorrelationDefinition;
import org.ziggrid.model.Enhancement;
import org.ziggrid.model.Grouping;
import org.ziggrid.utils.collections.CircularList;
import org.ziggrid.utils.collections.ListMap;
import org.ziggrid.utils.metrics.CodeHaleMetrics;
import org.ziggrid.utils.sync.SyncUtils;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.couchbase.client.CouchbaseClient;

public class CorrelationLocalProcessor implements LocalProcessor {
	public enum ProcState { NEEDGET, WAITGET, WAITSAVE, COMPLETE };
	
	public class ItemProcessor {
		private ProcState state = ProcState.COMPLETE;
		private List<Object> key;
		private List<JSONObject> entries;
		private String id;
		private OperationFuture<CASValue<Object>> fcas;
		private CASValue<Object> cas;
		private OperationFuture<Boolean> addr;
		private OperationFuture<CASResponse> casr;
		private String jks;
		
		public boolean isComplete() {
			return state == ProcState.COMPLETE;
		}

		public boolean begin() {
			// Go off and get the next entry if any
			synchronized (allEntries) {
				if (allEntries.isEmpty())
					return false;
				key = linkedKeys.iterator().next();
				entries = allEntries.removeAll(key);
				linkedKeys.remove(key);
			}

			JSONArray jk = new JSONArray();
			for (Object k : key)
				jk.put(k);
			jks = jk.toString();

			id = materializer.computeSHAId(table+"-factors", jks);
			activeProcessors++;
			state = ProcState.NEEDGET;
			return true;
		}

		public void advance() {
			switch (state) {
			case NEEDGET:
			{
				fcas = conn.asyncGets(id);
				state = ProcState.WAITGET;
				break;
			}
			case WAITGET:
			{
				if (!fcas.isDone())
					return;
				try {
					cas = fcas.get();
				} catch (Exception ex) {
					logger.error(ex.getMessage());
					state = ProcState.NEEDGET;
					return;
				}
				final Map<String, double[]> userRecord = new HashMap<String, double[]>();
				if (cas != null) {
					// recover factors from disk
					readFromDisk(userRecord, (String) cas.getValue());
				}
		
				// Apply all the entries
				for (JSONObject o : entries) {
					JSONArray corr = new JSONArray();
					try {
						for (Enhancement e : cd.items)
							corr.put(enhancer.process(e, o));
						String corrS = corr.toString();
						Object v = enhancer.process(cd.value, o);
						double value;
						if (v instanceof Integer)
							value = (Integer)v;
						else if (v instanceof Double)
							value = (Double)v;
						else {
							logger.error("Cannot handle value " + v);
							continue;
						}
						addTo(myGlobal, corrS, value);
						addTo(global, corrS, value);
						addTo(userRecord, corrS, value);
					} catch (Exception ex) {
						logger.error(ex.getMessage());
					}
				}
				try {
					JSONObject dump = asJSON(userRecord);
					logger.debug("Saving correlation factors " + id);
					if (cas == null) {
						addr = conn.add(id, 0, dump.toString());
						casr = null;
					} else {
						addr = null;
						casr = conn.asyncCAS(id, cas.getCas(), dump.toString());
					}
					materializer.materializeCorrelationObject(cd, table, global, grouping, jks, userRecord);
				} catch (Exception ex) {
					logger.error("Something bad went wrong, claiming we're done, but results will be inaccurate", ex);
					complete();
				}
				state = ProcState.WAITSAVE;
				break;
			}
			case WAITSAVE:
			{
				try {
					if (addr != null && addr.isDone()) {
						if (addr.get())
							complete();
						else
							state = ProcState.NEEDGET;
					} else if (casr != null && casr.isDone()) {
						if (casr.get() == CASResponse.OK)
							complete();
						else
							state = ProcState.NEEDGET;
					}
				} catch (Exception ex) {
					logger.error("Something bad went wrong, claiming we're done, but results will be inaccurate", ex);
					complete();
				}
				break;
			}
			case COMPLETE:
			{
				// nothing to do, unless we decide to fold begin in here
				break;
			}
			}
		}

		public void complete() {
			state = ProcState.COMPLETE;
			activeProcessors--;
		}
	}

	private static final Logger logger = LoggerFactory.getLogger("CorrelationProcessor");
	private final MaterializeObjects materializer;
	private final CouchbaseClient conn;
	private final CorrelationDefinition cd;
	private final Grouping grouping;
	private final String table;
	private final ListMap<List<Object>, JSONObject> allEntries = new ListMap<List<Object>, JSONObject>();
	private final LinkedHashSet<List<Object>> linkedKeys = new LinkedHashSet<List<Object>>();
	private final Map<String, double[]> global = new HashMap<String, double[]>();
	private final Map<String, double[]> myGlobal;
	private final Map<Integer, Map<String, double[]>> sharedGlobals = new HashMap<Integer, Map<String, double[]>>();
	private final EnhancementVM enhancer = new EnhancementVM();
	private CircularList<ItemProcessor> active = new CircularList<ItemProcessor>(50);
	private int activeProcessors = 0;
//	private static final Timer correlationProcessorTimer = CodeHaleMetrics.metrics.timer("CorrelationProcessorTimer");
//	private static final Meter correlationProcessorMeter = CodeHaleMetrics.metrics.meter("CorrelationProcessorMeter");
	private final int poolSize;
	private final int poolId;
	private boolean needGlobalRefresh = false;
	private final Map<Integer, GetFuture<Object>> waitingFor = new HashMap<Integer, GetFuture<Object>>();

	public CorrelationLocalProcessor(CouchbaseClient conn, MaterializeObjects materializer, CorrelationDefinition cd, Grouping grp, int poolSize, int poolId) {
		this.conn = conn;
		this.materializer = materializer;
		this.cd = cd;
		grouping = grp;
		this.poolSize = poolSize;
		this.poolId = poolId;
		table = cd.getViewName(grp);
		for (int i=1;i<=poolSize;i++) {
			HashMap<String, double[]> tmp = new HashMap<String, double[]>();
			String data = (String) conn.get(table+"-factors/" + poolId);
			if (data != null)
				readFromDisk(tmp, data);
			sharedGlobals.put(i, tmp);
		}
		myGlobal = sharedGlobals.get(poolId);
		globalRefresh();
		createMetrics();
		while (active.get() == null) {
			active.set(new ItemProcessor());
			active = active.getNext();
		}
	}

	@Override
	public Object keyFor(String key, JSONObject obj) {
		logger.debug("Correlation " + table + " obtaining key for " + key + " from " + obj);
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
			synchronized (allEntries) {
				while (allEntries.isEmpty() && activeProcessors == 0) {
					SyncUtils.waitFor(allEntries, 0);
				}
			}
			if (needGlobalRefresh)
				globalRefresh();
			loopOnce(active.get());
			active = active.getNext();
		}
	}

	private void globalRefresh() {
		needGlobalRefresh = false;
		if (!waitingFor.isEmpty()) {
			for (Entry<Integer, GetFuture<Object>> x : waitingFor.entrySet()) {
				if (!x.getValue().isDone()) {
					logger.error("Did not retrieve globals for " + x.getKey());
					waitingFor.clear();
					return;
				}
				else {
					try {
						readFromDisk(sharedGlobals.get(x.getKey()), (String)x.getValue().get());
					} catch (Exception ex) {
						logger.error("Error retrieving shared globals " + x.getKey(), ex);
						waitingFor.clear();
						return;
					}
				}
			}
			global.clear();
			for (Entry<Integer, Map<String, double[]>> ge : sharedGlobals.entrySet()) {
				for (Entry<String, double[]> ke : ge.getValue().entrySet()) {
					double[] pair = global.get(ke.getKey());
					if (pair == null) {
						pair = new double[] {0.0, 0.0};
						global.put(ke.getKey(), pair);
					}
					pair[0] += ke.getValue()[0];
					pair[1] += ke.getValue()[1];
				}
			
			}
		}
		waitingFor.clear();
		for (int i=1;i<=poolSize;i++) {
			if (i == poolId) continue;
			GetFuture<Object> data = conn.asyncGet(table+"-factors/" + poolId);
			waitingFor.put(poolId, data);
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
			try {
				conn.set(table+"-factors/" + poolId, 0, asJSON(myGlobal).toString());
				needGlobalRefresh  = true;
			} catch (JSONException e) {
				logger.error(e.getMessage());
			}
		}
	}

	private void loopOnce(ItemProcessor proc) {
		if (proc.isComplete()) {
			if (!proc.begin())
				return;
		}
		proc.advance();
	}

//	private void process(List<Object> key, List<JSONObject> entries) {
//		final Timer.Context correlationProcessorContext = correlationProcessorTimer.time();
//		logger.debug("Correlation processing " + key);
//
//		correlationProcessorContext.stop();
//		correlationProcessorMeter.mark(1);
//	}
//
	public JSONObject asJSON(final Map<String, double[]> userRecord) throws JSONException {
		JSONObject dump = new JSONObject();
		for (Entry<String, double[]> ur : userRecord.entrySet()) {
			JSONObject line = new JSONObject();
			line.put("sum", ur.getValue()[0]);
			line.put("count", ur.getValue()[1]);
			dump.put(ur.getKey(), line);
		}
		return dump;
	}

	public void readFromDisk(final Map<String, double[]> map, String obj) {
		try {
			JSONObject factors = new JSONObject(obj);
			@SuppressWarnings("unchecked")
			Iterator<String> it = factors.keys();
			while (it.hasNext()) {
				String s = it.next();
				JSONObject vs = factors.getJSONObject(s);
				double[] d = new double[] { vs.getDouble("sum"), vs.getDouble("count") };
				map.put(s, d);
			}
		} catch (Exception ex) {
			logger.error(ex.getMessage());
			System.exit(1);
		}
	}

	private void addTo(Map<String, double[]> map, String corr, double value) {
		if (!map.containsKey(corr))
			map.put(corr, new double[] { 0, 0});
		double[] arr = map.get(corr);
		arr[0] += value;
		arr[1] ++;
	}

	@Override
	public void bump() {
	}
	
	@Override
	public String toString() {
		return "Correlating " + cd.name + " from " + cd.from;
	}

	public String toThreadName() {
		return "Corr-" + cd.name + "-" + cd.from + grouping.asGroupName();
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
