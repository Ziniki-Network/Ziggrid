package org.ziggrid.driver;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.model.CorrelationDefinition;
import org.ziggrid.model.EnhancementDefinition;
import org.ziggrid.model.Grouping;
import org.ziggrid.model.LeaderboardDefinition;
import org.ziggrid.model.Model;
import org.ziggrid.model.SnapshotDefinition;
import org.ziggrid.model.SummaryDefinition;
import org.ziggrid.parsing.ErrorHandler;
import org.ziggrid.parsing.JsonReader;
import org.ziggrid.utils.collections.CollectionUtils;
import org.ziggrid.utils.exceptions.UtilException;
import org.ziggrid.utils.utils.Crypto;
import org.ziggrid.utils.utils.FileUtils;

import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.CouchbaseConnectionFactory;
import com.couchbase.client.CouchbaseConnectionFactoryBuilder;
import com.couchbase.client.protocol.views.Paginator;
import com.couchbase.client.protocol.views.Query;
import com.couchbase.client.protocol.views.Stale;
import com.couchbase.client.protocol.views.View;
import com.couchbase.client.protocol.views.ViewResponse;
import com.couchbase.client.protocol.views.ViewRow;

public class MaterializeObjects {
	private static final Logger logger = LoggerFactory.getLogger("MaterializeObjects");
	private static List<String> listTable = CollectionUtils.listOf("table");
	private static List<String> listCorrelation = CollectionUtils.listOf("correlation", "count");
	private CouchbaseClient conn;

	public static void main(String[] args) {
        if (args.length < 5) {
			System.err.println("Usage: define couchUrl dir bucket documentName model");
			System.exit(1);
		}
		String couchUrl = args[0];
		File dir = new File(args[1]);
		File model = FileUtils.combine(dir, args[2], args[3], args[4]);
		String txt = FileUtils.readFile(model);
		CouchbaseClient conn = openCouch(couchUrl, args[2]);
		try {
			new MaterializeObjects(conn).run(couchUrl, args[3], txt);
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			logger.error("Materialize shutting down couchbase connection");
			conn.shutdown(1, TimeUnit.MINUTES);
		}
	}
	
	public MaterializeObjects(CouchbaseClient conn) {
		this.conn = conn;
	}
	
	private void run(String couchUrl, String docid, String txt) {
		ErrorHandler eh = new ErrorHandler();
		JsonReader jr = new JsonReader();
		Model model = jr.readModel(eh, docid, txt);
		if (eh.displayErrors())
			return;
//		model.prettyPrint(new PrintWriter(System.out));

		for (final EnhancementDefinition e : model.enhancers(docid)) {
			materializeEnhancement(docid, e);
		}
		
		for (final SummaryDefinition s : model.summaries(docid)) {
			materializeSummary(docid, model, s);
		}
		
		for (final LeaderboardDefinition s : model.leaderboards(docid)) {
			materializeLeaderboard(docid, model, s);
		}
		
		for (final CorrelationDefinition s : model.correlations(docid)) {
			materializeCorrelation(docid, model, s);
		}
	}

	public void materializeEnhancement(String doc, final EnhancementDefinition e) {
		String type = e.to;
		String viewName = "enhance_"+type+"_from_"+e.from;
		logger.debug("Enhancing using " + viewName);
		View view = conn.getView(doc, viewName);
		Query q = new Query();
		q.setStale(Stale.FALSE);
		int rowsPerPage = 100;
		Paginator pg = conn.paginatedQuery(view, q, rowsPerPage);
		int page = 1;
		while (pg.hasNext()) {
			ViewResponse response = pg.next();
			logger.debug("Page " + page++ + " of " + rowsPerPage + " rows");
			for (ViewRow row : response)
				materialize(logger, type, row.getKey(), null, null, e.fieldNames, row.getValue());
		}
	}

	public void materializeSummary(String doc, Model model, SummaryDefinition s) {
		String reduceView = s.getViewName();
		View view = conn.getView(doc, reduceView);
		for (int keylen = s.matches.size();keylen >=0; keylen--) {
			String storeType = s.summary+(keylen==s.matches.size()?"":"-key"+keylen);
			Query q = new Query();
			q.setStale(Stale.FALSE);
			q.setGroupLevel(keylen);
			q.setReduce(true);
			ViewResponse pg = conn.query(view, q);
			for (ViewRow row : pg) {
				String id = computeSHAId(storeType, row.getKey());
				materialize(logger, storeType, id, s.keyFields(keylen), row.getKey(), s.valueFields(), row.getValue());
			}
		}
	}

	public void materializeLeaderboard(String doc, Model model, LeaderboardDefinition s) {
		for (Grouping g : s.groupings()) {
			View view = conn.getView(doc, s.getViewName(g));
			System.out.println("looking at view " + view.getViewName());
			int card = g.fields.size();
			List<String> matching = new ArrayList<String>();
			if (card > 0) {
				Query q = new Query();
				q.setReduce(true);
				q.setStale(Stale.FALSE);
				q.setGroupLevel(card);
				ViewResponse resp = conn.query(view, q);
				for (ViewRow row : resp) {
					matching.add(row.getKey());
				}
			} else
				matching.add(null);
			for (String key : matching) {
				Query q = new Query();
				q.setReduce(false);
				q.setLimit(s.top);
				if (key != null) {
					String end = key.substring(0, key.length()-1) + ",\"\\uefff\"]";
					if (!s.ascending) { 
						q.setRange(end, key);
						q.setDescending(true);
					} else
						q.setRange(key, end);
				} else if (!s.ascending)
					q.setDescending(true);
				System.out.println(q);
				ViewResponse resp = conn.query(view, q);
				try {
					materializeLeaderboardObject(s, view.getViewName(), g, key, resp);
				} catch (JSONException ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	public void materializeLeaderboardObject(LeaderboardDefinition defn, String viewName, Grouping grouping, String ks, ViewResponse response) throws JSONException {
		JSONArray storeAs = new JSONArray();
		for (ViewRow row : response) {
			// TODO: these types are probably variable
			JSONArray arr = new JSONArray(row.getKey());
			String tmp = row.getValue();
			JSONArray value;
			try {
				value = new JSONArray(tmp);
			} catch (JSONException ex) {
				value = new JSONArray();
				value.put(tmp);
			}
			int ignore = grouping.fields.size();
			JSONArray thisRow = new JSONArray();
			for (int i=0;i<defn.sorts.size();i++) {
				logger.debug("sort by: " + defn.sorts.get(i) + " = " + arr.getString(i+ignore));
				thisRow.put(arr.getString(i+ignore));
			}
			for (int i=0;i<defn.values.size();i++) {
				logger.debug("value: " + defn.values.get(i) + " = " + value.get(i));
				thisRow.put(value.get(i));
			}
			storeAs.put(thisRow);
		}
		JSONArray valueValues = new JSONArray();
		valueValues.put(storeAs);
		String id = computeSHAId(viewName, ks);
		materialize(logger, viewName, id, grouping.fields, ks, listTable, valueValues.toString());
	}

	public void materializeCorrelation(String doc, Model model, CorrelationDefinition s) {
		// First we need to read in the correlation coefficients
		Map<String, double[]> global = new HashMap<String, double[]>();
		{
			View view = conn.getView(doc, s.getGlobalViewName());
			Query q = new Query();
			q.setReduce(true);
			q.setStale(Stale.FALSE);
			q.setGroupLevel(s.items.size());
			ViewResponse resp = conn.query(view, q);
			for (ViewRow r : resp) {
				try {
					JSONObject v = new JSONObject(r.getValue());
					global.put(r.getKey(), new double[] { v.getDouble("sum"), v.getDouble("count") });
				} catch (JSONException ex) {
					ex.printStackTrace();
				}
			}
			System.out.println("global map has " + global.size() + " keys");
		}
		for (Grouping g : s.groupings()) {
			View view = conn.getView(doc, s.getViewName(g));
			System.out.println("looking at view " + view.getViewName());
			int card = g.fields.size();
			List<String> matching = new ArrayList<String>();
			{
				Query q = new Query();
				q.setReduce(true);
				q.setStale(Stale.FALSE);
				q.setGroupLevel(card);
				ViewResponse resp = conn.query(view, q);
				for (ViewRow row : resp) {
					matching.add(row.getKey());
				}
			}
			for (String key : matching) {
				Query q = new Query();
				q.setReduce(true);
				q.setStale(Stale.FALSE);
				q.setGroupLevel(card + s.items.size());
				String end = key.substring(0, key.length()-1) + ",\"\\uefff\"]";
				q.setRange(key, end);
				ViewResponse resp = conn.query(view, q);
				try {
					Map<String, double[]> me = new HashMap<String, double[]>();
					for (ViewRow r : resp) {
						JSONObject stats = new JSONObject(r.getValue());
						me.put(r.getKey(), new double[] { stats.getDouble("sum"), stats.getDouble("count") });
					}
					materializeCorrelationObject(s, view.getViewName(), global, g, key, me);
				} catch (JSONException ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	public void materializeCorrelationObject(CorrelationDefinition s, String viewName, Map<String, double[]> global, Grouping grouping, String key, Map<String, double[]> curr) throws JSONException {
		double corr = 0.0;
		double total = 0;
		for (Entry<String, double[]> r : curr.entrySet()) {
//			System.out.print(r.getValue());
			double[] pair = global.get(r.getKey());
			if (pair == null) {
				logger.error("Didn't find " + r.getKey() + " in global correlation map");
				continue;
			}
			double[] stats = r.getValue();
			double d = (pair[0] * stats[0])/pair[1];  // this is global-average * #usertimes
//			System.out.println(pair[0] + "/" + pair[1] + " = " + d);
			corr += d;
			total += stats[1];
		}
		String id = computeSHAId(viewName, key);
		logger.debug("Materializing correlation object as " + id);
		JSONArray values = new JSONArray();
		values.put(corr/total);
		values.put(total);
		materialize(logger, viewName, id, grouping.fields, key, listCorrelation, values.toString());
	}

	public void materializeSnapshotObject(SnapshotDefinition sd, List<String> keyFields, List<String> valueFields, int start, int endAt, String viewName, String key, JSONObject factors) throws JSONException {
		double[] top = new double[valueFields.size()];
		double[] bottom = new double[valueFields.size()];
		
		for (int w = start; w<=endAt;w++) {
			String ws = Integer.toString(w);
			if (!factors.has(ws))
				continue;
			JSONObject o = factors.getJSONObject(ws);
			double d = sd.figureDecay(endAt, w);
			if (d <= 0.0)
				continue;
			for (int i=0;i<valueFields.size();i++) {
				String s = valueFields.get(i);
				try {
					top[i] += d*o.getDouble(s);
					bottom[i] += d;
				} catch (Exception ex) {
					logger.error(ex.getMessage());
				}
			}
		}
		String id = computeSHAId(viewName, key);
		try {
			JSONArray values = new JSONArray();
			for (int i=0;i<top.length;i++)
				values.put(top[i]/bottom[i]);
			materialize(logger, viewName, id, keyFields, key, valueFields, values.toString());
		} catch (JSONException ex) {
			logger.error("Could not save " + id + ": " + ex.getMessage());
		}
	}

	public void materialize(Logger logger, String type, String id, List<String> keyFields, String firstKey, List<String> valueFields, String thenValue) {
		logger.debug("materializing " + id + " with " + (firstKey != null?firstKey  + " ":"") + thenValue);
		try {
			JSONObject storeAs = new JSONObject();
			storeAs.put("id", id);
			storeAs.put("ziggridType", type);
			if (firstKey != null) {
				int idx = 0;
				JSONArray items = new JSONArray(firstKey);
				for (String f : keyFields) {
					storeAs.put(f, items.get(idx++));
				}
			}
			if (thenValue != null) {
				int idx = 0;
				JSONArray items = new JSONArray(thenValue);
				for (String f : valueFields) {
					storeAs.put(f, items.get(idx++));
				}
			}
			conn.set(id, 0, storeAs.toString());
		} catch (JSONException ex) {
			ex.printStackTrace();
		}
	}
	
	/* Deprecated due to duplication
	public void materialize(String type, String id, List<String> keyFields, JSONArray firstKey, List<String> valueFields, JSONArray thenValue) {
		logger.debug("materializing " + id + " with " + (firstKey != null?firstKey  + " ":"") + thenValue);
		try {
			JSONObject storeAs = new JSONObject();
			storeAs.put("id", id);
			storeAs.put("ziggridType", type);
			if (firstKey != null) {
				int idx = 0;
				for (String f : keyFields) {
					storeAs.put(f, firstKey.get(idx++));
				}
			}
			if (thenValue != null) {
				int idx = 0;
				for (String f : valueFields) {
					storeAs.put(f, thenValue.get(idx++));
				}
			}
			conn.set(id, 0, storeAs.toString());
		} catch (JSONException ex) {
			ex.printStackTrace();
		}
	}
	*/
	
	String computeSHAId(String table, String key) {
		if (key == null)
			return table;
		return table + "-" + Crypto.hash(key);
	}

	public static CouchbaseClient openCouch(String couchUrl, String bucket) {
		try {
			URI server = new URI(couchUrl+"pools");
			List<URI> serverList = new ArrayList<URI>();
			serverList.add(server);
			CouchbaseConnectionFactoryBuilder builder = new CouchbaseConnectionFactoryBuilder();
			builder.setOpTimeout(30000);
			builder.setTimeoutExceptionThreshold(30000);
			CouchbaseConnectionFactory ccf = builder.buildCouchbaseConnection(serverList, bucket, "");
			return new CouchbaseClient(ccf);
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}
}
