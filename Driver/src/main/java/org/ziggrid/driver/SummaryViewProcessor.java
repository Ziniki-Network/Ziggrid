package org.ziggrid.driver;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.exceptions.ZiggridException;
import org.ziggrid.model.MatchField;
import org.ziggrid.model.SummaryDefinition;
import org.ziggrid.utils.collections.ListMap;
import org.ziggrid.utils.metrics.CodeHaleMetrics;
import org.ziggrid.utils.sync.SyncUtils;
import org.ziggrid.utils.utils.DateUtils;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.couchbase.client.protocol.views.Query;
import com.couchbase.client.protocol.views.Stale;
import com.couchbase.client.protocol.views.ViewResponse;
import com.couchbase.client.protocol.views.ViewRow;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/** This is the version of the SummaryProcessor that uses the Couchbase View
 * Infrastructure, but it is just too slow.
 * 
 * Also, it is out of date and has some leakage from the LocalProcessor in it.
 */
public class SummaryViewProcessor implements ViewProcessor {
	private final Logger logger;
	private final List<SummaryQuery> queries;
	private final MaterializeObjects materializer;
	private int workerCount;
	private ListeningExecutorService fixedThreadPool;
	private final SummaryDefinition trailblazer;
	private final List<String> valueFields;
	// Object here is the "summary key" and the list is of JSONObjects that match that key
	// Not all reduction rules may match the key
	private final ListMap<List<Object>, JSONObject> allEntries = new ListMap<List<Object>, JSONObject>();
	// This is a list of keys in FIFO order
	private final LinkedHashSet<List<Object>> linkedKeys = new LinkedHashSet<List<Object>>();

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
			doProcessing(keys, firstKey, lastKey);
		}
	}

	public SummaryViewProcessor(List<SummaryQuery> queries, MaterializeObjects materializer, int workerCount) {
		this.queries = queries;
		this.materializer = materializer;
		this.workerCount = workerCount;
		fixedThreadPool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(workerCount));
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
		while (true) {
			List<Object> key;
			List<JSONObject> entries;
			synchronized (allEntries) {
				while (allEntries.isEmpty())
					SyncUtils.waitFor(allEntries, 0);
				key = linkedKeys.iterator().next();
				linkedKeys.remove(key);
				entries = allEntries.removeAll(key);
			}
//			processWithViews(keys);
		}
	}
	
	@Override
	public void spill(Set<Object> keys) {
		throw new ZiggridException("Cannot process using views");
		// TODO: make this work with threading
//		processWithViews(keys);
	}
	
	@SuppressWarnings({ "unchecked", "unused" })
	private void processWithViews(Set<Object> keys) {
		List<List<Object>> overList = new ArrayList<List<Object>>();
		for (Object o : keys)
			overList.add((List<Object>) o);
		Date from = new Date();
		List<ListenableFuture<?>> dispatchSummaryWorkers = dispatchSummaryWorkers(overList);
		try {
			Futures.successfulAsList(dispatchSummaryWorkers).get();
		} catch (InterruptedException e) {
			logger.error("Interrupted while waiting for SummaryProcessing workers", e);
		} catch (ExecutionException e) {
			logger.error("ExecutionException while waiting for SummaryProcessing workers", e);
		}
		logger.info("Summary processing took " + DateUtils.elapsedTime(from, new Date(), DateUtils.Format.sss3) + " for " + overList.size() + " keys");
	}

	private List<ListenableFuture<?>> dispatchSummaryWorkers(List<List<Object>> overList) {
		List<ListenableFuture<?>> taskFutures = new ArrayList<ListenableFuture<?>>();
		
		int itemCount = overList.size();
		int itemsPerWorker = itemCount / workerCount;
		int leftOvers = itemCount % workerCount;
		int startIndex = 0;
		int endIndex = itemCount - 1;

		int currentStartIndex = startIndex;
		int currentEndIndex = startIndex + itemsPerWorker - 1;

		for (int i = 0; i < workerCount; i++) {
			if (i < leftOvers)
				currentEndIndex++;
			taskFutures.add(fixedThreadPool.submit(new SummaryWorker(overList, currentStartIndex, currentEndIndex)));
			if (currentEndIndex == endIndex)
				return taskFutures;
			else {
				currentStartIndex = currentEndIndex + 1;
				currentEndIndex = Math.min(currentEndIndex + itemsPerWorker, endIndex);
			}
		}
		return taskFutures;
	}

	public void doProcessing(List<List<Object>> keys, int firstKey, int lastKey) {
		logger.info("looking at keys = " + keys.subList(firstKey, lastKey));
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
					if (response.size() != 1)
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
		return "Summarizing to " + trailblazer.summary + " from " + trailblazer.event;
	}
	
	
	@Override
	public String toThreadName() {
		return "Sum-" + trailblazer.summary + "-" + trailblazer.event;
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
