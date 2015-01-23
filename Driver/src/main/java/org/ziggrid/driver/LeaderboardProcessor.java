package org.ziggrid.driver;

public class LeaderboardProcessor { /* implements ViewProcessor {
	private static final Logger logger = LoggerFactory.getLogger("LeaderboardProcessor");
	private final CouchQuery query;
	private final MaterializeObjects materializer;
	private final LeaderboardDefinition defn;
	private final Grouping grouping;
	private final Set<List<Object>> allKeys = new LinkedHashSet<List<Object>>();
	private boolean bumpRequested;
	private static final Timer leaderboardProcessorTimer = CodaHaleMetrics.metrics.timer("LeaderboardProcessorTimer");
	private static final Meter leaderboardProcessorMeter = CodaHaleMetrics.metrics.meter("LeaderboardProcessorMeter");

	public LeaderboardProcessor(CouchQuery query, MaterializeObjects materializer, LeaderboardDefinition defn, Grouping grouping) {
		this.query = query;
		this.materializer = materializer;
		this.defn = defn;
		this.grouping = grouping;
		createMetrics();
	}

	@Override
	public Object keyFor(String key, JSONObject obj) {
		logger.debug("Leaderboard obtaining key for " + key + " from " + obj);
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
				arr = gather(SnapshotViewProcessor.groupSize);
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

	public void process(Set<List<Object>> set) {
		final Timer.Context leaderboardProcessorContext = leaderboardProcessorTimer.time();
		logger.debug(this + " processing " + set);
		
		for (List<Object> ks : set) {
			Query q = new Query();
			q.setStale(Stale.OK);
			q.setReduce(false);
			q.setLimit(defn.top);
			JSONArray start = new JSONArray();
			JSONArray end = new JSONArray();
			for (Object k : ks) {
				start.put(k);
				end.put(k);
			}
			end.put("\uefff");
			if (!defn.ascending) { 
				q.setRange(end.toString(), start.toString());
				q.setDescending(true);
			} else
				q.setRange(start.toString(), end.toString());
			
			try {
				ViewResponse response = query.query(q);
				logger.debug("Couchbase response is: " + response);
				JSONArray fieldsValues = new JSONArray();
				for (Object v : ks) {
					fieldsValues.put(v);
				}
				materializer.materializeLeaderboardObject(defn, query.getViewName(), grouping, fieldsValues.toString(), response);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		leaderboardProcessorContext.stop();
		leaderboardProcessorMeter.mark(set.size());
	}

	@Override
	public void bump() {
		bumpRequested = true;
	}
	
	@Override
	public String toString() {
		return "LeaderboardProcessor[" + defn.name + "]";
	}

	public String toThreadName() {
		return "Lead-" + defn.name + grouping.asGroupName();
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
*/
	}
