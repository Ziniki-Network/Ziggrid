package org.ziggrid.driver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jettison.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.api.StoreableObject;
import org.ziggrid.kvstore.KVStorageEngine;
import org.ziggrid.kvstore.KVStore;
import org.ziggrid.kvstore.KVTransaction;
import org.ziggrid.kvstore.QueuedItem;
import org.ziggrid.model.MatchField;
import org.ziggrid.model.Model;
import org.ziggrid.model.OpReductionWithNoFields;
import org.ziggrid.model.OpReductionWithOneField;
import org.ziggrid.model.Reduction;
import org.ziggrid.model.SummaryDefinition;
import org.zinutils.utils.Crypto;

/** This is an "ActingQueue", managed by a single-thread "ActingObserver" which
 * handles summarizing records.
 *
 * <p>
 * &copy; 2014 Gareth Powell.  All rights reserved.
 *
 * @author Gareth Powell
 *
 */
public class ActingSummarizer extends ActingProcessor {
	protected static final Logger logger = LoggerFactory.getLogger("Summarizer");
	private final String name;
	private final List<MatchField> matches;
	private final Map<String, Reduction> reductions;

	public ActingSummarizer(Model model, KVStorageEngine store, SummaryDefinition sd, int max) {
		super(store, "Summarizer");
		this.name = sd.summary + (max == sd.matches.size() ? "" : "-key" + max);
		addWatchable(this.name);
		this.matches = new ArrayList<MatchField>();
		for (int i=0;i<max;i++)
			this.matches.add(sd.matches.get(i));
		reductions = sd.reductions;
	}

	@Override
	public StoreableObject canYouProvide(Object inTx, String watchable, Map<String, Object> options) {
		try {
			KVTransaction tx = (KVTransaction) inTx;
			JSONArray key = new JSONArray();
			for (MatchField mf : matches) {
				if (!options.containsKey(mf.summaryField))
					return null;
				key.put(options.get(mf.summaryField));
			}
			return tx.get(this.name + "_" + Crypto.hash(key.toString()));
		} catch (Exception ex) {
			return null;
		}
	}

	@Override
	protected void processMessage(KVTransaction tx, KVStore store, QueuedItem qi) {
		/* Believed out of date
		String baseKey = oid.substring(0, oid.length()-3);
		StoreableObject old = null; 
		String hasoldKey = baseKey+"hasold";
		String hasOld = tx.getString(hasoldKey);
		if (hasOld == null)
			logger.info("There was no old version of object to delete: " + hasoldKey);
		else if (hasOld.equals("yes")) {
			old = tx.get(baseKey+"old");
			System.out.println("TODO: need to remove the effects of " + old);
		}
		tx.deleteString(hasoldKey);
		*/
		String sid = (String) qi.get("_summaryId");
		
		StoreableObject summary = null;
		boolean isNew = false;
		int redCount = 0;
		if (tx.contains(sid)) {
			summary = tx.get(sid);
			redCount = (Integer) summary.get("_reductionCount");
		}
		else {
			isNew = true;
			summary = new StoreableObject(name, sid);
			for (MatchField mf : matches) {
				summary.set(mf.summaryField, qi.get(mf.summaryField));
			}
		}
		summary.set("_reductionCount", redCount+1);
		for (Entry<String, Reduction> kv : reductions.entrySet()) {
			Reduction r = kv.getValue();
			String summField = kv.getKey();
			if (r instanceof OpReductionWithNoFields) {
				OpReductionWithNoFields r0 = (OpReductionWithNoFields)r;
				if (r0.op.equals("++")) {
					int k = 0;
					if (!isNew)
						try { k = (Integer) summary.get(summField);	} catch (Exception ex) {}
					summary.set(summField, k+1);
				} else
					System.out.println("Can't handle " + r0.op);
			} else if (r instanceof OpReductionWithOneField) {
				OpReductionWithOneField r1 = (OpReductionWithOneField)r;
				if (r1.op.equals("+=")) {
					int k = 0;
					if (!isNew)
						try { k = (Integer) summary.get(summField);	} catch (Exception ex) {}
					Object ev = qi.get(summField);
					if (ev instanceof Boolean)
						ev = ((Boolean)ev)?1:0;
					summary.set(summField, k+(Integer)ev);
				} else
					System.out.println("Can't handle " + r1.op);
			}
		}
		store.write(summary);
	}
	
	@Override
	public String toString() {
		return "ActingSummarizer[" + watchables() + "]";
	}
}
