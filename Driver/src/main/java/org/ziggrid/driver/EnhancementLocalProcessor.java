package org.ziggrid.driver;

public class EnhancementLocalProcessor { /*implements LocalProcessor {
	private static final Logger logger = LoggerFactory.getLogger("EnhancementProcessor");
	private final MaterializeObjects materializer;
	private final EnhancementDefinition defn;
	private static final Timer enhancementProcessorTimer = CodaHaleMetrics.metrics.timer("EnhancementProcessorTimer");
	private static final Meter enhancementProcessorMeter = CodaHaleMetrics.metrics.meter("EnhancementProcessorMeter");
	private final Map<String, JSONObject> allEntries = new HashMap<String, JSONObject>();
	private final LinkedHashSet<String> linkedKeys = new LinkedHashSet<String>();
	private final EnhancementVM enhancer = new EnhancementVM();
	
	public EnhancementLocalProcessor(MaterializeObjects materializer, EnhancementDefinition defn) {
		this.materializer = materializer;
		this.defn = defn;
		createMetrics();
	}

	@Override
	public Object keyFor(String key, JSONObject obj) {
		return key;
	}


	@Override
	public void run() {
		while (true) {
			String key;
			JSONObject entry;
			synchronized (allEntries) {
				while (allEntries.isEmpty())
					SyncUtils.waitFor(allEntries, 0);
				key = linkedKeys.iterator().next();
				linkedKeys.remove(key);
				entry = allEntries.remove(key);
			}
			process(key, entry);
		}
	}
	
	@Override
	public void spill(Set<Object> keys) {
		throw new ZiggridException("Cannot process using views");
	}

	@Override
	public void spill(ListMap<Object, JSONObject> keyedEntries) {
		synchronized (allEntries) {
			if (!allEntries.isEmpty()) {
				logger.error(this + ".spill() called with " + keyedEntries.totalSize() + " new entries, when there are still " + allEntries.size() + " waiting to be processed");
			}
			for (Object k1 : keyedEntries.keySet()) {
				String k = (String) k1;
				linkedKeys.add(k);
				if (keyedEntries.size(k) != 1)
					logger.error("There should only be one entry for key " + k1);
				allEntries.put(k, keyedEntries.get(k).get(0));
			}
			allEntries.notify();
		}
	}

	private void process(String key, JSONObject entry) {
		final Timer.Context enhancementProcessorContext = enhancementProcessorTimer.time();
		String rkey = defn.to + "_from_"+key;
		JSONArray value = new JSONArray();
		try {
			for (String fn : defn.fieldNames) {
				Enhancement enhancement = defn.fields.get(fn);
				value.put(enhancer.process(enhancement, entry));
			}
			materializer.materialize(logger, defn.to, rkey, null, null, defn.fieldNames, value.toString());
		} catch (Exception ex) {
			logger.error(ex.getMessage());
		}
		enhancementProcessorContext.stop();
		enhancementProcessorMeter.mark(1);
	}

	@Override
	public void bump() {
	}
	
	@Override
	public String toString() {
		return "Enhancing " + defn.from + " to " + defn.to;
	}

	public String toThreadName() {
		return "Enh-" + defn.from + "-" + defn.to;
	}
	
	private void createMetrics() {
		CodaHaleMetrics.metrics.register(MetricRegistry.name(this.toThreadName() + "-EntryGauge"),
				new Gauge<Integer>() {
			@Override
			public Integer getValue() {
				synchronized (allEntries) {
					return allEntries.size();
				}
			}
		});
	}
*/
}

