package org.ziggrid.driver.enhancers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.ziggrid.api.StoreableObject;
import org.ziggrid.driver.EnhancementVM;
import org.ziggrid.kvstore.KVTransaction;
import org.ziggrid.kvstore.Sender;
import org.ziggrid.model.CorrelationDefinition;
import org.ziggrid.model.Enhancement;
import org.ziggrid.model.Grouping;
import org.ziggrid.model.NamedEnhancement;
import org.zinutils.metrics.CodaHaleMetrics;
import org.zinutils.utils.Crypto;
import org.zinutils.utils.StringUtil;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

public class CorrelationDefinitionMapper implements DefinitionMapper {
	private final String sha;
	private final List<NamedEnhancement> items;
	private final Enhancement correlateTo;
	private final Grouping grp;
	public final String name;
	private static final Timer correlationDMTimer = CodaHaleMetrics.metrics.timer("CorrelationDMTimer");

	public CorrelationDefinitionMapper(CorrelationDefinition cd, String sha, Grouping g) {
		this.sha = sha;
		this.name = cd.name + g.asGroupName();
		items = cd.items;
		correlateTo = cd.value;
		grp = g;
	}

	@Override
	public void process(EnhancementVM evm, Sender sender, KVTransaction tx, StoreableObject prev, StoreableObject item) throws JSONException {
		Context processTimerContext = correlationDMTimer.time();
		JSONObject json = item.asJsonObject();
		byte[] icid = indivId(evm, json);
		String gcid = globId(evm, json);
		Object oo = evm.process(correlateTo, json);
		double outcome = 0.5;
		if (oo instanceof Double)
			outcome = (Double)oo;
		else if (oo instanceof Integer)
			outcome = (Integer)oo;

		// Now (over) write new version while writing out old version
		Map<String,Object> fields = new HashMap<String, Object>();
		fields.put("_indiv", StringUtil.hex(icid));
		fields.put("_global", gcid);
		fields.put("_outcome", outcome);
		for (String f : grp.fields) {
			fields.put(f, json.get(f));
		}
		for (NamedEnhancement mf : items) {
			fields.put(mf.name, evm.process(mf.enh, json));
		}
		sender.queue(tx, sha, icid, fields);
		processTimerContext.stop();
	}
	
	private byte[] indivId(EnhancementVM evm, JSONObject json) throws JSONException {
		JSONArray key = new JSONArray();
		for (String f : grp.fields)
			key.put(json.get(f));
		return Crypto.computeHash(key.toString());
	}
	
	private String globId(EnhancementVM evm, JSONObject json) throws JSONException {
		JSONArray key = new JSONArray();
		for (NamedEnhancement mf : items)
			key.put(evm.process(mf.enh, json));
		return Crypto.hash(key.toString());
	}
	
	@Override
	public String toString() {
		return "CorrelationMapper[" + sha + "]";
	}
}
