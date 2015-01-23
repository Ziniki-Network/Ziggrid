package org.ziggrid.driver.interests;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.api.IInterestEngine;
import org.ziggrid.api.StorageEngine;
import org.ziggrid.api.StoreableObject;
import org.ziggrid.driver.ActingProcessor;
import org.ziggrid.driver.ObserverDisconnectedException;
import org.ziggrid.driver.ObserverListener;
import org.ziggrid.model.Model;
import org.ziggrid.model.ObjectDefinition;
import org.zinutils.collections.ListMap;

public class InterestEngine implements IInterestEngine {
	public final static Logger logger = LoggerFactory.getLogger("Interests");
	private final Model model;
	private final Map<String, ObjectDefinition> defns = new HashMap<String, ObjectDefinition>();
	private final Map<ObserverListener, Interest> byListener = new HashMap<ObserverListener, Interest>();
	private final Map<String, InterestTable> concerns = new HashMap<String, InterestTable>();
	private final ListMap<String, ActingProcessor> processors = new ListMap<String, ActingProcessor>();
	private final StorageEngine store;

	public InterestEngine(StorageEngine store, Model model) {
		this.store = store;
		this.model = model;
		for (String bucket : model.definitions.keySet())
			for (ObjectDefinition od : model.objects(bucket))
				defns.put(od.name.toLowerCase(), od);
	}

	public void processorExists(ActingProcessor proc) {
		for (String s : proc.watchables()) {
			processors.add(s.toLowerCase(), proc);
		}
	}

	public synchronized void watch(ObserverListener lsnr, String table, Map<String, Object> options) {
		logger.info("Starting to watch " + table + " with options: " + options);
		try {
			String tlc = table.toLowerCase();
			if (!defns.containsKey(tlc)) {
				lsnr.sendError("there is no table " + table);
				return;
			}
			/** Rightly or wrongly, this is actually not a problem, since it can be something pushed in directly, e.g. GameDate 
			if (!processors.contains(tlc)) {
				lsnr.sendError("nothing generates table " + table);
				return;
			}
			*/
			ObjectDefinition od = defns.get(tlc);
			Interest i = new Interest(lsnr, table, od, options);
			logger.info("Adding " + i);
			byListener.put(lsnr, i);
			if (!concerns.containsKey(tlc))
				concerns.put(tlc, new InterestTable());
			InterestTable it = concerns.get(tlc);
			it.addInterested(i);
			for (Entry<String, Object> o : options.entrySet())
				it.requireField(o.getKey(), o.getValue(), i);
			
			// See if somebody can generate this and if they can if we can find an instance in the KV store
			// Deliver that straight away
			StoreableObject so = store.findExisting(processors, tlc, options);
			if (so != null)
				lsnr.deliver(so.asPayload());
			/* TODO:
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
			*/
		} catch (Exception ex) {
			logger.error("Could not create interest: ", ex);
			lsnr.sendError(ex.getMessage());
		}
	}

	public synchronized void unwatch(ObserverListener lsnr) {
		Interest i = byListener.get(lsnr);
		if (i == null)
			return;
		logger.info("Stopping watching handle = " + lsnr);
		i.cleanUp();
		byListener.remove(lsnr);
	}

	public void process(Collection<StoreableObject> updatedObjects) {
		Set<Interest> deadInterests = new HashSet<Interest>();
		for (StoreableObject so : updatedObjects) {
			String t = (String) so.get("ziggridType");
			if (t == null)
				continue;
			t = t.toLowerCase();
			if (!concerns.containsKey(t))
				continue;
			for (Interest i : concerns.get(t).qualify(so)) {
				try {
					i.lsnr.deliver(so.asPayload());
//				} catch (JSONException ex) {
//					logger.error("JSON Exception in delivery: " + ex.getMessage());
				} catch (ObserverDisconnectedException ex) {
					logger.error("Client has gone away; deleting interest");
					deadInterests.add(i);
				}
			}
		}
		for (Interest i : deadInterests)
			i.cleanUp();
	}

	public Model getModel() {
		return model;
	}

	public Set<String> getWatchables() {
		// TODO Auto-generated method stub
		return null;
	}
}
