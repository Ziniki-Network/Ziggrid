package org.ziggrid.driver;


public class CompositeProcessor { /*implements LocalProcessor {
	private static final Logger logger = LoggerFactory.getLogger("CompositeProcessor");
	private final CouchbaseClient conn;
	private final CompositeDefinition defn;
	private final ListMap<String, JSONObject> allEntries = new ListMap<String, JSONObject>();
	private final LinkedHashSet<String> linkedKeys = new LinkedHashSet<String>();
	private final EnhancementVM evm = new EnhancementVM();

	public CompositeProcessor(CouchbaseClient conn, CompositeDefinition defn) {
		this.conn = conn;
		this.defn = defn;
		createMetrics();
	}

	@Override
	public Object keyFor(String key, JSONObject obj) {
		try {
			logger.debug("Composite obtaining key for " + key + " from " + obj);
			StringBuilder ret = new StringBuilder();
			for (KeyElement ke : defn.keys) {
				ret.append(ke.extract(obj));
			}
			logger.debug("Returning key " + ret);
			return ret.toString();
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	@Override
	public void run() {
		while (true) {
			String key;
			List<JSONObject> entries;
			synchronized (allEntries) {
				while (allEntries.isEmpty())
					SyncUtils.waitFor(allEntries, 0);
				key = linkedKeys.iterator().next();
				linkedKeys.remove(key);
				entries = allEntries.removeAll(key);
			}
			process(key, entries);
		}
	}
	
	@Override
	public void spill(Set<Object> keys) {
	}
	
	@Override
	public void spill(ListMap<Object, JSONObject> keyedEntries) {
		synchronized (allEntries) {
			if (!allEntries.isEmpty())
				logger.error(this + ".spill() called with " + keyedEntries.totalSize() + " new entries, when there are still " + allEntries.totalSize() + " waiting to be processed");
			for (Object k1 : keyedEntries.keySet()) {
				String k = (String) k1;
				linkedKeys.add(k);
				for (JSONObject o : keyedEntries.get(k1))
					allEntries.add(k, o);
			}
			allEntries.notify();
		}
	}

	public void process(String k, List<JSONObject> events) {
		boolean processed = false;
		while (!processed) {
			try {
				CASValue<Object> cas = conn.gets(k);
				JSONObject s;
				if (cas == null) {
					s = new JSONObject();
					s.put("ziggridType", defn.into);
				} else {
					s = new JSONObject((String)cas.getValue());
				}			

				for (JSONObject event : events) {
					for (NamedEnhancement f : defn.fields) {
						try {
							s.put(f.name, evm.process(f.enh, event));
						} catch (JSONException ex) {
							ex.printStackTrace();
						}
					}
				}

				if (cas == null) {
					Boolean r = conn.add(k, 0, s.toString()).get();
					if (r)
						processed = true;
					else
						logger.error("Add failed for " + k + "; trying read again");
				} else {
					CASResponse r = conn.cas(k, cas.getCas(), s.toString());
					if (r == CASResponse.OK)
						processed = true;
					else
						logger.error("CAS Failed for " + k + "; trying again");
				}
			} catch (Exception ex) {
				logger.error("Something bad went wrong, claiming we're done, but results will be inaccurate", ex);
				processed = true; // lies
			}
		}
	}
	
	@Override
	public void bump() {
	}
	
	@Override
	public String toString() {
		return "CompositeProcessor[" + defn + "]";
	}

	@Override
	public String toThreadName() {
		return "Comp-" + defn;
	}

	private void createMetrics() {
		CodaHaleMetrics.metrics.register(MetricRegistry.name(this.toThreadName() + "-EntryGauge"),
                new Gauge<Integer>() {
		            @Override
		            public Integer getValue() {
		            	synchronized (allEntries) {
		            		return allEntries.totalSize();
		            	}
		            }
		        });
	}
	*/
}
