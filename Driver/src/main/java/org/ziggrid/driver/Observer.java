package org.ziggrid.driver;

import java.net.SocketException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.couchbase.DefineViews;
import org.ziggrid.exceptions.ZiggridException;
import org.ziggrid.model.CompositeDefinition;
import org.ziggrid.model.CorrelationDefinition;
import org.ziggrid.model.EnhancementDefinition;
import org.ziggrid.model.Grouping;
import org.ziggrid.model.LeaderboardDefinition;
import org.ziggrid.model.Model;
import org.ziggrid.model.ObjectDefinition;
import org.ziggrid.model.SnapshotDefinition;
import org.ziggrid.model.SummaryDefinition;
import org.ziggrid.parsing.ErrorHandler;
import org.ziggrid.parsing.ProcessingMethods;
import org.ziggrid.taptomq.TapDataPacket;
import org.ziggrid.utils.collections.ListMap;
import org.ziggrid.utils.exceptions.UtilException;
import org.ziggrid.utils.sync.SyncUtils;

import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.CouchbaseConnectionFactory;
import com.couchbase.client.CouchbaseConnectionFactoryBuilder;

public class Observer implements BucketDelayLine.Processor<ViewProcessor, Object>, Runnable {
	public class FutureDelivery {
		private Interest i;
		private String ltype;
		private JSONObject obj;

		public FutureDelivery(Interest i, String ltype, JSONObject obj) {
			this.i = i;
			this.ltype = ltype;
			this.obj = obj;
		}

		public void deliverMe(ListMap<String, Interest> removeDead) {
			deliver(removeDead, i, ltype, obj);
		}
	}

	private static final int summaryProcessorWorkerCount = 50;
	private static final Logger logger = LoggerFactory.getLogger("Observer");
	private MaterializeObjects materializer;
	private CouchbaseClient conn;
	private final List<MessageSource> messageSources = new ArrayList<MessageSource>();
	private final ListMap<String, ViewProcessor> processors = new ListMap<String, ViewProcessor>();
	private String couchUrl;
	private String bucket;
	private Model model;
	private BucketDelayLine<ViewProcessor, Object> delay;
	private int total;
	// TODO: Only for testing; do not change this again here - make this a parameter, default of -1
	private int max = -1;
	private final ListMap<String, Interest> interests = new ListMap<String, Interest>();
	private final Map<Integer, List<Interest>> handleInterests = new HashMap<Integer, List<Interest>>();
	private final int gateLength;
	private final TreeMap<String, Integer> gating = new TreeMap<String, Integer>();
	private int now = 0;
	private final ListMap<Integer, FutureDelivery> futures = new ListMap<Integer, FutureDelivery>();
	private final TreeMap<String, Integer> record = new TreeMap<String, Integer>();
	private final ProcessingMethods pm;

	public Observer(String couchUrl, String bucket, Model model, MessageSource messageSource) {
		this.couchUrl = couchUrl;
		this.bucket = bucket;
		this.pm = new ProcessingMethods();
		this.model = model;
		this.messageSources.add(messageSource);
		this.gateLength = 0;
	}

	public Observer(String couchUrl, String bucket, ProcessingMethods pm, Model model, Collection<MessageSource> sources, int gateLength, TreeMap<String, Integer> gating) {
		this.couchUrl = couchUrl;
		this.bucket = bucket;
		this.pm = pm;
		this.model = model;
		this.messageSources.addAll(sources);
		this.gateLength = gateLength;
		for (Entry<String, Integer> e : gating.entrySet())
			this.gating.put(e.getKey().toLowerCase(), e.getValue());
	}

	public Model getModel() {
		return model;
	}

	public void createViews() {
		conn = openCouch(couchUrl, bucket);
		try {
			String json = (String)conn.get("ziggridConfig");
			Set<String> documentsToLoad = new HashSet<String>(model.documents());
			JSONObject config;
			boolean checkLoad = true;
			if (System.getProperty("org.ziggrid.config.load") != null && System.getProperty("org.ziggrid.config.load").equals("always"))
				checkLoad = false;
			if (!checkLoad || json == null) {
				config = new JSONObject();
			} else {
				config = new JSONObject(json);
				logger.info("Have found existing config object " + config);
				for (String s : model.documents())
					if (config.has(s) && model.isSHA(s, config.getString(s))) {
						logger.info("Document " + s + " has not changed");
						documentsToLoad.remove(s);
					}
			}
			if (!documentsToLoad.isEmpty()) {
				logger.info("Loading (changed) documents: " + documentsToLoad);
				DefineViews loader = new DefineViews();
				ErrorHandler eh = new ErrorHandler();
				for (String doc : documentsToLoad) {
					loader.loadDesignDocument(eh, couchUrl + "couchBase/" + bucket, pm, model, doc);
					config.put(doc, model.getSHA(doc));
				}
				if (eh.displayErrors())
					return;
				conn.set("ziggridConfig", 0, config.toString());
			}
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}
	
	public void run() {
		try {
			createInfrastructure();
			createProcessors();
			for (MessageSource ms : messageSources)
				ms.startMessageFlow();
			
			while (!messageSources.isEmpty()) {
				for (MessageSource ms : messageSources) {
					if (!ms.hasMoreMessages()) {
						messageSources.remove(ms);
						break;
					}
					
					try {
						TapDataPacket msg = getNextMessage(ms);
						if (msg == null) continue;
						if (!processMessage(msg)) continue;
						if (max != -1 && max-- <= 0) {
							logger.info("Reached maximum number of records; draining and exiting");
							messageSources.clear();
							break;
						}
					} catch (Exception ex) {
						logger.error("Exception bubbled all the way up to run()", ex);
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			close();
		}
	}

	public void createInfrastructure() {
		delay = new BucketDelayLine<ViewProcessor, Object>(this, 10, 1000); // have to open couch first otherwise we can hit NPEs
		for (MessageSource ms : messageSources)
			ms.initialize();
		materializer = new MaterializeObjects(conn);
	}

	public void createProcessors() {
		for (String documentName : model.documents()) {
			for (EnhancementDefinition eh : model.enhancers(documentName)) {
				if (pm.enhanceWithView)
					processors.add(eh.from, new EnhancementViewProcessor(new CouchQuery(conn, documentName, eh.getViewName()), materializer, eh));
				else
					processors.add(eh.from, new EnhancementLocalProcessor(materializer, eh));
			}
			for (SummaryDefinition sh : model.summaries(documentName)) {
				if (pm.summarizeWithView) {
					if (sh.trailblazer()) {
						List<SummaryDefinition> summaries = model.getSummaries(sh.summary, sh.event);
                        List<SummaryQuery> queries = new ArrayList<SummaryQuery>();
                        for (SummaryDefinition is : summaries)
                            queries.add(new SummaryQuery(is, new CouchQuery(conn, documentName, is.getViewName())));
                        processors.add(sh.event, new SummaryViewProcessor(queries, materializer, summaryProcessorWorkerCount));
					}
				} else
					processors.add(sh.event, new SummaryLocalProcessor(conn, model, materializer, sh, summaryProcessorWorkerCount));
			}
			for (LeaderboardDefinition lh : model.leaderboards(documentName)) {
				for (Grouping grp : lh.groupings()) {
					LeaderboardProcessor proc = new LeaderboardProcessor(new CouchQuery(conn, documentName, lh.getViewName(grp)), materializer, lh, grp);
					processors.add(lh.from, proc);
				}
			}
			for (CorrelationDefinition cd : model.correlations(documentName)) {
				for (Grouping grp : cd.groupings()) {
					ViewProcessor proc;
					if (pm.correlateWithView)
						proc = new CorrelationViewProcessor(new CouchQuery(conn, documentName, cd.getGlobalViewName()), new CouchQuery(conn, documentName, cd.getViewName(grp)), materializer, cd, grp);
					else {
						// The two integers here are "pool size" (i.e. number of nodes) and "pool id" (i.e. my place) on scale 1-size.
						proc = new CorrelationLocalProcessor(conn, materializer, cd, grp, 1, 1);
					}
					processors.add(cd.from, proc);
				}
			}
			for (SnapshotDefinition sd : model.snapshots(documentName)) {
				ViewProcessor proc;
				if (pm.snapshotWithView)
					proc = new SnapshotViewProcessor(new CouchQuery(conn, documentName, sd.getViewName()), materializer, sd);
				else
					proc = new SnapshotLocalProcessor(conn, materializer, sd);
				processors.add(sd.from, proc);
			}
			for (CompositeDefinition cd : model.composites(documentName)) {
				CompositeProcessor cp = new CompositeProcessor(conn, cd);
				processors.add(cd.from, cp);
			}
		}
		for (ViewProcessor proc : processors.values()) {
			logger.info("Creating processor " + proc + " in thread " + proc.toThreadName());
			new Thread(proc, proc.toThreadName()).start();
		}
	}

	public TapDataPacket getNextMessage(MessageSource ms) {
		int prev = total/1000;
		TapDataPacket msg = ms.getNextMessage();
		if (msg != null) {
			total++;
			return msg;
		}
		
		logger.debug("Timed out waiting for next tap message");
		if (total/1000 > prev) {
			logger.info("Handled " + " total tap messages");
		}
		return null;
	}

	private boolean processMessage(TapDataPacket msg) throws JSONException {
		String key = msg.getKey();
		String value = msg.getValue();
		JSONObject obj;
		try {
			obj = new JSONObject(value);
		} catch (JSONException ex) {
			logger.error("Object was not json: " + key);
			return false;
		}
		if (!obj.has("ziggridType")) {
			logger.debug("Ignoring object " + key + " without ziggridType");
			return false;
		}			
		String type = obj.getString("ziggridType");
		if (type == null)
			return false;
		synchronized (record ) {
			int val = 0;
			if (record.containsKey(type))
				val = record.get(type);
			record.put(type, val+1);
		}
		String ltype = type.toLowerCase();
		if (interests.contains(ltype)) {
			ListMap<String, Interest> removeDead = new ListMap<String, Interest>();
			for (Interest i : interests.get(ltype))
				if (i.appliesTo(obj)) {
					if (gateLength == 0)
						deliver(removeDead, i, ltype, obj);
					else {
						Integer already = gating.get(ltype);
						if (already == null)
							logger.error("There was no gate for " + ltype + " in " + gating);
						else
							futures.add(now+this.gateLength-already, new FutureDelivery(i, ltype, obj));
					}
				}
			cleanUp(removeDead);
		}
		if (!processors.contains(type))
			return false;
		for (ViewProcessor p : processors.get(type)) {
			logger.debug("Handling tap update for " + key + " of type " + type);
			try {
				Object keyFor = p.keyFor(key, obj);
				if (keyFor == null)
					continue;
				if (p instanceof LocalProcessor)
					delay.doNext(p, keyFor, obj);
				else
					delay.add(p, keyFor);
			} catch (Exception ex) {
				logger.error("Error processing message " + key + " of type " + type + " using " + p, ex);
			}
		}
		return true;
	}

	public void deliver(ListMap<String, Interest> removeDead, Interest i, String ltype, JSONObject obj) {
		try {
			i.deliver(obj);
		} catch (Exception ex) {
			Throwable e2 = UtilException.unwrap(ex);
			if (e2 instanceof SocketException) {
				logger.error("Deleting dead connection " + i);
				removeDead.add(ltype, i);
			} else
				logger.error("Error delivering message ", ex);
		}
	}

	public void cleanUp(ListMap<String, Interest> removeDead) {
		for (String r : removeDead)
			for (Interest i : removeDead.get(r))
				interests.remove(r, i);
	}

	@Override
	public void spill(ViewProcessor proc, Set<Object> keys) {
		try {
			proc.spill(keys);
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}
	
	@Override
	public void spill(ViewProcessor proc, ListMap<Object, JSONObject> map) {
		((LocalProcessor)proc).spill(map);
	}

	public CouchbaseClient openCouch(String couchUrl, String bucket) {
		try {
			URI server = new URI(couchUrl+"pools");
			List<URI> serverList = new ArrayList<URI>();
			serverList.add(server);
			CouchbaseConnectionFactoryBuilder builder = new CouchbaseConnectionFactoryBuilder();
			builder.setOpTimeout(30000);
			builder.setTimeoutExceptionThreshold(30000);
			CouchbaseConnectionFactory ccf = builder.buildCouchbaseConnection(serverList, bucket, "");
			for (int i=0;i<10;i++) {
				try {
					return new CouchbaseClient(ccf);
				} catch (Exception ex) {
					logger.error("Could not connect to couch ... retrying");
					SyncUtils.sleep(100);
				}
			}
			throw new ZiggridException("Could not connect to Couchbase");
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	@Override
	public void allProcessed(Set<ViewProcessor> keySet) {
		// Issue proclamation on the TAPs we've processed ...
		synchronized (record) {
			if (!record.isEmpty()) {
				logger.info("TAP processed: " + record);
				record.clear();
			}
		}
		// The future is now ... deliver anything we've been gating up for a while (or just a moment)
		if (futures.contains(now)) {
			logger.info("Delivering everything queued until " + now);
			ListMap<String, Interest> removeDead = new ListMap<String, Interest>();
			for (FutureDelivery fd : futures.get(now))
				fd.deliverMe(removeDead);
			cleanUp(removeDead);
		}
		// advance the clock anyway
		now++;
		
		// 
		for (String s : processors)
			for (ViewProcessor p : processors.get(s))
				if (!keySet.contains(p))
					p.bump();
	}

	public void close() {
		if (delay != null)
			delay.drain();
		for (MessageSource ms : messageSources)
			ms.shutdown();
		if (conn != null) {
			logger.error("Shutting down main couchbase connection");
			conn.shutdown();
		}
	}

	public boolean watch(ObserverSender sender, int h, String table, Map<String, Object> options) {
		List<Interest> newInterests = new ArrayList<Interest>();
		System.out.println();
		for (String bucket : model.definitions.keySet()) {
			for (ObjectDefinition od : model.objects(bucket)) {
				if (od.name.equalsIgnoreCase(table)) {
					try {
						Interest i = new Interest(materializer, sender, h, table, od, options);
						logger.info("Adding interest " + i + " with key " + i.sha + " isFull: " + i.haveFullKey);
						newInterests.add(i);
						
						// See if somebody can generate this and if they can if we can find an instance in the KV store
						// Deliver that straight away
						if (i.haveFullKey) {
							String curr = (String)conn.get(i.sha);
							if (curr != null)
								try {
									sender.deliver(i, new JSONObject(curr));
								} catch (JSONException ex) {
									logger.error("Object " + i.sha + " was not JSON", ex);
								}
						}
					} catch (ZiggridException ex) {
						logger.error("Could not create interest: ", ex);
						throw ex;
					}
				}
			}
		}
		if (newInterests.isEmpty())
			return false;
		this.handleInterests.put(h, newInterests);
		for (Interest i : newInterests)
			interests.add(table.toLowerCase(), i);
		return true;
	}

	public void unwatch(int hs) {
		if (handleInterests.containsKey(hs)) {
			List<Interest> x = handleInterests.remove(hs);
			for (Interest i : x) {
				interests.remove(i.table.toLowerCase(), i);
			}
		}
	}
}
