package org.ziggrid.driver;

public class SummaryLocalProcessor { /*implements LocalProcessor {
	public class SummaryState {
		private int grp;
		private String storeType;
		private String shaId;
		public OperationFuture<CASValue<Object>> fcas;
		public CASValue<Object> cas;
		public JSONObject s;
		public OperationFuture<Boolean> addr;
		public OperationFuture<CASResponse> casr;
		public boolean done;

		public SummaryState(int grp, String storeType, String shaId) {
			this.grp = grp;
			this.storeType = storeType;
			this.shaId = shaId;
		}
	}

	public enum ProcState { NEEDGET, WAITGET, WAITSAVE, COMPLETE };

	public class ItemProcessor {
		private ProcState state = ProcState.COMPLETE;
		private List<Object> key;
		private List<JSONObject> events;
		private ArrayList<SummaryState> summaries;
		
		public boolean isComplete() {
			return state == ProcState.COMPLETE;
		}

		public boolean begin() {
			// Go off and get the next entry if any
			synchronized (allEntries) {
				if (allEntries.isEmpty())
					return false;
				key = linkedKeys.iterator().next();
				events = allEntries.removeAll(key);
				linkedKeys.remove(key);
			}

			summaries = new ArrayList<SummaryState>();
			for (int grp = key.size();grp>=1;grp--) { // TODO: should also be 0, but that causes too much contention
				String storeType = defn.summary+(grp==key.size()?"":"-key"+grp);
				List<Object> actualKey = key.subList(0, grp);
				JSONArray key = new JSONArray();
				for (Object o : actualKey)
					key.put(o);
				String shaId = materializer.computeSHAId(storeType, key.toString());
				logger.debug("Summarizing into " + shaId + " which is " + storeType + " with " + key);
				summaries.add(new SummaryState(grp, storeType, shaId));
			}
			
			state = ProcState.NEEDGET;
			return true;
		}

		public void advance() {
			switch (state) {
			case NEEDGET:
			{
				for (SummaryState ss : summaries)
					ss.fcas = conn.asyncGets(ss.shaId);

				state = ProcState.WAITGET;
				break;
			}
			case WAITGET:
			{
				logger.debug("Summary applying to " + key + " with " + summaries.size() + " summaries and " + events.size() + " events");
				for (SummaryState ss : summaries)
					if (!ss.fcas.isDone())
						return;
				for (SummaryState ss : summaries) {
					if (ss.done) {
						logger.debug("Ignoring repeated application of " + ss.storeType + " for " + key);
						continue;
					}
					logger.debug("Summarizing into " + ss.storeType);
					try {
						ss.cas = ss.fcas.get();
						if (ss.cas == null) {
							// Create a new object if needed
							ss.s = new JSONObject();
							ss.s.put("ziggridType", ss.storeType);
							int pos = 0;
							for (MatchField mf : defn.matches)
								if (pos < ss.grp)
									ss.s.put(mf.summaryField, key.get(pos++));
						} else {
							// read the appropriate object
							ss.s = new JSONObject((String)ss.cas.getValue());
						}                

						for (FieldDefinition x : summaryObjectDefn.fields()) {
							if (ss.s.has(x.name)) continue;
							if (defn.reductions.containsKey(x.name)) {
								if (x.type.equals("number"))
									ss.s.put(x.name, 0);
								else if (x.type.equals("string"))
									ss.s.put(x.name, "");
								else
									logger.error("What is " + x.type);
							}
						}

						for (JSONObject event : events) {
							logger.debug("Summarizing event " + event);
							boolean process = true;
							for (int pf=0;pf<ss.grp;pf++) {
								if (!match(defn.matches.get(pf), ss.s, event)) {
									process = false;
									break;
								}
							}
							if (!process)
								continue;
							for (Entry<String, Reduction> r : defn.reductions.entrySet()) {
								try {
									reduce(ss.s, event, r.getKey(), r.getValue());
								} catch (JSONException ex) {
									ex.printStackTrace();
								}
							}
						}
						
						if (ss.cas == null) {
							ss.addr = conn.add(ss.shaId, 0, ss.s.toString());
							ss.casr = null;
						} else {
							ss.addr = null;
							ss.casr = conn.asyncCAS(ss.shaId, ss.cas.getCas(), ss.s.toString());
						}
					} catch (Exception ex) {
						logger.error(ex.getMessage());
						state = ProcState.NEEDGET;
						return;
					}
				}
				state = ProcState.WAITSAVE;
				logger.debug("Summary moving " + key + " into WAITSAVE state " + state);
				break;
			}
			case WAITSAVE:
			{
				try {
					logger.debug("Summary looking at " + key + " in WAITSAVE state");
					boolean allDone = true;
					for (SummaryState ss : summaries) {
						if (ss.done)
							continue;
						if (ss.addr != null && ss.addr.isDone()) {
							if (ss.addr.get())
								ss.done = true;
							else {
								logger.info("Add failed for " + key + " in " + ss.storeType);
								allDone = false;
							}
						} else if (ss.casr != null && ss.casr.isDone()) {
							CASResponse casResponse = ss.casr.get();
							if (casResponse == CASResponse.OK)
								ss.done = true;
							else {
								logger.info("CAS Failed for" + key + " in " + ss.storeType + " was: " + casResponse);
								allDone = false;
							}
						} else
							return;
					}
					if (allDone)
						complete();
					else
						state = ProcState.NEEDGET;
					logger.debug("Summary advancing with allDone=" + allDone + " state=" + state);
				} catch (Exception ex) {
					logger.error("Something bad went wrong, claiming we're done, but results will be inaccurate", ex);
					complete();
				}
				break;
			}
			case COMPLETE:
			{
				logger.debug("Summary in COMPLETE  state");
				// nothing to do, unless we decide to fold begin in here
				break;
			}
			}
		}

		public void complete() {
			state = ProcState.COMPLETE;
		}
	}

	private final Logger logger;
	private final MaterializeObjects materializer;
	private final CouchbaseClient conn;
	// Object here is the "summary key" and the list is of JSONObjects that match that key
	// Not all reduction rules may match the key
	private final ListMap<List<Object>, JSONObject> allEntries = new ListMap<List<Object>, JSONObject>();
	// This is a list of keys in FIFO order
	private final LinkedHashSet<List<Object>> linkedKeys = new LinkedHashSet<List<Object>>();
	private final SummaryDefinition defn;
	private ObjectDefinition summaryObjectDefn;
	private CircularList<ItemProcessor> active = new CircularList<ItemProcessor>(50);

	public SummaryLocalProcessor(CouchbaseClient conn, Model model, MaterializeObjects materializer, SummaryDefinition defn, int workerCount) {
		this.conn = conn;
		this.materializer = materializer;
		this.defn = defn;
		String lname = "SummaryProcessor " + defn.getViewName();
		this.logger = LoggerFactory.getLogger(lname);
		createMetrics();
		ErrorHandler eh = new ErrorHandler();
		summaryObjectDefn = model.getModel(eh, defn.summary);
		while (active.get() == null) {
			active.set(new ItemProcessor());
			active = active.getNext();
		}
	}
	
	@Override
	public Object keyFor(String key, JSONObject event) {
		try {
			List<Object> ret = new ArrayList<Object>();
			for (MatchField mf : defn.matches)
				ret.add(event.get(mf.eventField));
			return ret;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
	@Override
	public void run() {
		while (true) {
			synchronized (allEntries) {
				while (allEntries.isEmpty())
					SyncUtils.waitFor(allEntries, 0);
			}
			loopOnce(active.get());
			active = active.getNext();
		}
	}
	
	private void loopOnce(ItemProcessor proc) {
		if (proc.isComplete()) {
			if (!proc.begin())
				return;
		}
		proc.advance();
	}

	@Override
	public void spill(Set<Object> keys) {
		throw new ZiggridException("Cannot process using views");
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

	private boolean match(MatchField mf, JSONObject summary, JSONObject event) {
		try {
			Object sf = summary.get(mf.summaryField);
			Object ef = event.get(mf.eventField);
			return sf.equals(ef);
		} catch (JSONException ex) {
			logger.error("JSON exception thrown", ex);
			return false;
		}
	}

	// Apply a reduction by hand
	private void reduce(JSONObject summary, JSONObject event, String summaryField, Reduction r) throws JSONException {
		if (r instanceof OpReductionWithNoFields) {
			if (((OpReductionWithNoFields) r).op.equals("++"))
				summary.put(summaryField, summary.getInt(summaryField)+1);
			else
				logger.error("Cannot process reduction " + r);
		} else if (r instanceof OpReductionWithOneField) {
				OpReductionWithOneField ofr = (OpReductionWithOneField) r;
				if (ofr.op.equals("+=")) {
					int evf = 0;
					Object o = event.get(ofr.eventField);
					if (o instanceof Integer)
						evf = (Integer)o;
					else if (o instanceof Boolean)
						evf = ((Boolean)o)?1:0;
					else if (o instanceof String)
						evf = Integer.parseInt((String)o);
					else
						logger.error("Cannot handle += of " + o.getClass().getName());
					summary.put(summaryField, summary.getInt(summaryField)+evf);
				} else
					logger.error("Cannot process reduction " + r);
		} else
			logger.error("Cannot process reduction " + r);
	}

	@Override
	public void bump() {
	}
	
	@Override
	public String toString() {
		return "Summarizing to " + defn.summary + " from " + defn.event;
	}
	
	
	@Override
	public String toThreadName() {
		return "Sum-" + defn.summary + "-" + defn.event + defn.whichString();
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
