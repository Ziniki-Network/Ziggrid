package org.ziggrid.driver;

/** This is the version of the SummaryProcessor that uses the Couchbase View
 * Infrastructure, but it is just too slow.
 */
public class SummaryViewProcessor { /* implements ViewProcessor {
	private final Logger logger;
	private final List<SummaryQuery> queries;
	private final MaterializeObjects materializer;
	private int workerCount;
	private ExecutorService fixedThreadPool;
	private final SummaryDefinition trailblazer;
	private final List<String> valueFields;
	private int gaugeValue;
	protected final Object gaugeLock = new Object();

	private class SummaryWorker implements Runnable {

		private List<List<Object>> keys;
		private int firstKey;
		private int lastKey;
		
		public SummaryWorker(List<List<Object>> keys, int firstKey, int lastKey) {
			this.keys = keys;
			this.firstKey = firstKey;
			this.lastKey = lastKey;
		}
		
		@Override
		public void run() {
			try {
				doProcessing(keys, firstKey, lastKey);
			} catch (Throwable t) {
				logger.error("Error encountered doing summary", t);
			}
			synchronized (gaugeLock) {
				gaugeValue -= (lastKey-firstKey);
			}
		}
	}

	public SummaryViewProcessor(List<SummaryQuery> queries, MaterializeObjects materializer, int workerCount) {
		this.queries = queries;
		this.materializer = materializer;
		this.workerCount = workerCount;
		fixedThreadPool = Executors.newFixedThreadPool(workerCount);
		trailblazer = queries.get(0).defn;
		String lname = "SummaryProcessor " + trailblazer.getViewName();
		this.logger = LoggerFactory.getLogger(lname);
		if (queries.size() == 1)
			valueFields = queries.get(0).defn.valueFields();
		else {
			valueFields = new ArrayList<String>();
			for (SummaryQuery sq : queries)
				for (String s : sq.defn.valueFields())
					if (!valueFields.contains(s))
						valueFields.add(s);
		}
		createMetrics();
	}
	
	@Override
	public Object keyFor(String key, JSONObject event) {
		try {
			List<Object> ret = new ArrayList<Object>();
			for (MatchField mf : trailblazer.matches)
				ret.add(event.get(mf.eventField));
			return ret;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
	@Override
	public void run() {
		// We delegated all our work to the fixedThreadPool, so we can relax :-)
	}
	
	@Override
	public void spill(Set<Object> keys) {
		processWithViews(keys);
	}
	
	@SuppressWarnings({ "unchecked" })
	private void processWithViews(Set<Object> keys) {
		List<List<Object>> overList = new ArrayList<List<Object>>();
		for (Object o : keys)
			overList.add((List<Object>) o);
		synchronized (gaugeLock) {
			gaugeValue += overList.size();
		}
		dispatchSummaryWorkers(overList);
	}

	private void dispatchSummaryWorkers(List<List<Object>> overList) {
		int itemCount = overList.size();
		int itemsPerWorker = itemCount / workerCount;
		int leftOvers = itemCount % workerCount;
		int startIndex = 0;

		for (int i = 0; i < workerCount; i++) {
			if (startIndex >= itemCount)
				break;
			int endIndex = Math.min(startIndex + itemsPerWorker +  (i < leftOvers? 1: 0), itemCount);
			fixedThreadPool.submit(new SummaryWorker(overList, startIndex, endIndex));
			startIndex = endIndex;
		}
	}

	public void doProcessing(List<List<Object>> keys, int firstKey, int lastKey) {
		logger.info("Processing keys; size = " + keys.size() + " from " + firstKey + " until " + lastKey + ": " + keys.subList(firstKey, lastKey));
		for (int i=firstKey;i<lastKey;i++) {
			List<Object> k = keys.get(i);
			logger.debug("Reading summary entry for " + k);
			for (int grp=k.size();grp>=0;grp--) {
				String storeType = trailblazer.summary+(grp==k.size()?"":"-key"+grp);
				Query q = new Query();
				q.setStale(Stale.OK);
				q.setReduce(true);
				q.setGroupLevel(grp);
				JSONArray start = new JSONArray();
				JSONArray end = new JSONArray();
				for (int gi=0;gi<grp;gi++) {
					start.put(k.get(gi));
					end.put(k.get(gi));
				}
				if (grp < k.size())
					end.put("\uefff");
				q.setRange(start.toString(), end.toString());
				
				String key = null;
				TreeMap<String, Object> values = new TreeMap<String, Object>();
				for (SummaryQuery sq : queries) {
					ViewResponse response = sq.query.query(q);
					if (response.size() == 0)
						continue;
					else if (response.size() != 1)
						throw new ZiggridException("You didn't think about this enough " + response);
					for (ViewRow row : response) {
						try {
							if (key == null)
								key = row.getKey();
							else if (!key.equals(row.getKey()))
								throw new ZiggridException("Mismatched keys: " + key + " and " + row.getKey());
							JSONArray arr = new JSONArray(row.getValue());
							if (arr.length() != sq.defn.valueFields().size())
								throw new ZiggridException("Mismatched values: " + sq.defn.valueFields() + " and " + row.getValue());
							for (int j=0;j<arr.length();j++) {
								String vf = sq.defn.valueFields().get(j);
								if (!values.containsKey(vf))
									values.put(vf, arr.get(j));
								else {
									// TODO: we need to consider other possibilities
									// This is valid for ++ or +=
									int x = (Integer)values.get(vf);
									values.put(vf, x + arr.getInt(j));
								}
							}
						} catch (JSONException ex) { 
							ex.printStackTrace();
						}
					}
				}
				if (key == null)
					continue;
				String id = materializer.computeSHAId(storeType, key);
				logger.debug("Need to combine values for key " + key + " into " + values);
				JSONArray value = new JSONArray();
				for (String s : valueFields)
					value.put(values.get(s));
				materializer.materialize(logger, storeType, id, trailblazer.keyFields(grp), key, valueFields, value.toString());
			}
		}
	}

	@Override
	public void bump() {
		for (SummaryQuery q : queries)
			q.query.bump();
	}
	
	@Override
	public String toString() {
		return "View Summarizing to " + trailblazer.summary + " from " + trailblazer.event;
	}
	
	
	@Override
	public String toThreadName() {
		return "Sum-" + trailblazer.summary + "-" + trailblazer.event;
	}

	private void createMetrics() {
		CodaHaleMetrics.metrics.register(MetricRegistry.name(this.toThreadName() + "-EntryGauge"),
                new Gauge<Integer>() {
		            @Override
		            public Integer getValue() {
		            	synchronized (gaugeLock) {
		            		return gaugeValue;
		            	}
		            }
		        });
	}
*/
	}
