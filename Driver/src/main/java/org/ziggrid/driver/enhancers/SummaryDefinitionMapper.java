package org.ziggrid.driver.enhancers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.ziggrid.api.StoreableObject;
import org.ziggrid.driver.EnhancementVM;
import org.ziggrid.kvstore.KVTransaction;
import org.ziggrid.kvstore.Sender;
import org.ziggrid.model.MatchField;
import org.ziggrid.model.OpReductionWithOneField;
import org.ziggrid.model.Reduction;
import org.ziggrid.model.SummaryDefinition;
import org.zinutils.metrics.CodaHaleMetrics;
import org.zinutils.utils.Crypto;
import org.zinutils.utils.StringUtil;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

public class SummaryDefinitionMapper implements DefinitionMapper {
	private final String sha;
	public final String ofType;
	private final List<MatchField> matches;
	private final Map<String, Reduction> reductions;
	private static final Timer summaryDMTimer = CodaHaleMetrics.metrics.timer("SummaryDMTimer");

	public SummaryDefinitionMapper(SummaryDefinition sd, int k, String sha) {
		this.sha = sha;
		this.ofType = sd.summary + (k == sd.matches.size() ? "" : "-key" + k);
		this.matches = new ArrayList<MatchField>();
		for (int i=0;i<k;i++)
			this.matches.add(sd.matches.get(i));
		this.reductions = sd.reductions;
	}

	@Override
	public void process(EnhancementVM evm, Sender sender, KVTransaction tx, StoreableObject prev, StoreableObject item) throws JSONException {
		Context processTimerContext = summaryDMTimer.time();
		// The thing we are mapping here is the event object
		// Thus we can write a number of different inputs for the same output
		// But we do want to write it with the appropriate "ID" for the destination
		
		byte[] sid = summaryId(item);
//		String pref = "map/" + ofType + "/" + sha + "/" + sid + "/" + item.id();
		
		/** I think I want to deny all this
		 * This is based on the assumption we are reusing ids
		 * Since we are not anymore, we can presumably just leave it to the reducer
		// We need to see if "the old version" has already been written & if not signal it ourselves
		// Note: because prev is null the first time, we need to write some marker as well as the object itself
		// I think we also need this for tx-ial-ness.
		
		String oldId = null;
		if (!tx.containsString(pref+"/hasold")) {
			if (prev != null) {
				tx.put(pref+"/hasold", "yes");
				oldId = pref + "/old";
			} else
				tx.put(pref+"/hasold", "no");
		}
		 */

		// Now (over) write new version while writing out old version
//		String newId = pref + "/new";
		Map<String, Object> fields = new HashMap<String, Object>();
		fields.put("_summaryId", ofType + "_" + StringUtil.hex(sid));
		for (MatchField mf : matches) {
			fields.put(mf.summaryField, item.get(mf.eventField));
		}
		for (Entry<String, Reduction> fr : reductions.entrySet()) {
			Reduction r = fr.getValue();
			if (r instanceof OpReductionWithOneField) {
				// Process the RHS of the reduction and present the mapped result as the "value" of the field
				String f = ((OpReductionWithOneField)r).eventField;
				fields.put(fr.getKey(), item.get(f));
			}
		}
		sender.queue(tx, sha, sid, fields);
		processTimerContext.stop();
	}

	private byte[] summaryId(StoreableObject item) {
		JSONArray key = new JSONArray();
		for (MatchField mf : matches) {
			key.put(item.get(mf.eventField));
		}
		return Crypto.computeHash(key.toString());
	}
	
	@Override
	public String toString() {
		return "SummaryMapper[" + sha + "]";
	}
}
