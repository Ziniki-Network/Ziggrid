package org.ziggrid.driver;

public class SnapshotViewProcessor { /*implements ViewProcessor {
	private static final Logger logger = LoggerFactory.getLogger("SnapshotProcessor");
	private final CouchQuery query;
	private final SnapshotDefinition sd;
	private final List<String> keyFields = new ArrayList<String>();
	private final Set<List<Object>> allKeys = new LinkedHashSet<List<Object>>();
	private boolean bumpRequested;
	private static final Timer snapshotProcessorTimer = CodaHaleMetrics.metrics.timer("SnapshotProcessorTimer");
	private static final Meter snapshotProcessorMeter = CodaHaleMetrics.metrics.meter("SnapshotProcessorMeter");
	static final int groupSize = 50;

	public SnapshotViewProcessor(CouchQuery query, MaterializeObjects materializer, SnapshotDefinition sd) {
		this.query = query;
		this.sd = sd;
		for (NamedEnhancement expr : sd.group){
			keyFields.add(expr.name);
		}
		keyFields.add(sd.upTo.name);
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
				arr = gather(groupSize);
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
				
//				ViewResponse resp = query.query(q);
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
		return "View Snapshot of " + sd.from;
	}

	public String toThreadName() {
		return "Snap-" + sd.from;
	}
	
	private void createMetrics() {
		CodaHaleMetrics.metrics.register(MetricRegistry.name(this.toThreadName() + "-EntryGauge"),
				new Gauge<Integer>() {
			@Override
			public Integer getValue() {
				synchronized (allKeys) {
					return allKeys.size();
				}
			}
		});
	}
*/}
