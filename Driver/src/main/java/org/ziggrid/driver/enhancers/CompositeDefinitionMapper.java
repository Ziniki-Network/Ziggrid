package org.ziggrid.driver.enhancers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.ziggrid.api.StoreableObject;
import org.ziggrid.driver.EnhancementVM;
import org.ziggrid.kvstore.KVTransaction;
import org.ziggrid.kvstore.Sender;
import org.ziggrid.model.CompositeDefinition;
import org.ziggrid.model.NamedEnhancement;
import org.ziggrid.model.ObjectDefinition.KeyElement;
import org.zinutils.metrics.CodaHaleMetrics;
import org.zinutils.utils.Crypto;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

public class CompositeDefinitionMapper implements DefinitionMapper {
	private final String sha;
	private final List<KeyElement> keys;
	private final List<NamedEnhancement> fields;
	public String into;
	private static final Timer compositeDMTimer = CodaHaleMetrics.metrics.timer("CompositeDMTimer");
	
	public CompositeDefinitionMapper(CompositeDefinition cd, String sha) {
		this.sha = sha;
		this.into = cd.into;
		keys = cd.keys;
		fields = cd.fields;
	}

	@Override
	public void process(EnhancementVM evm, Sender sender, KVTransaction tx, StoreableObject prev, StoreableObject item) throws JSONException {
		Context processTimerContext = compositeDMTimer.time();
		JSONObject json;
		try {
			json = item.asJsonObject();
		} catch (JSONException ex) {
			ex.printStackTrace();
			throw ex;
		}
		String sid = compositeId(evm, json);
		Map<String,Object> mapped = new HashMap<String, Object>();
		mapped.put("_key", sid);
		for (NamedEnhancement f : fields) {
			mapped.put(f.name, evm.process(f.enh, json));
		}
		sender.queue(tx, sha, Crypto.computeHash(sid), mapped);
		processTimerContext.stop();
	}
	
	private String compositeId(EnhancementVM evm, JSONObject json) throws JSONException {
		StringBuilder sb = new StringBuilder();
		for (KeyElement k : keys) {
			sb.append(k.extract(json));
		}
		return sb.toString();
	}
	
	@Override
	public String toString() {
		return "CompositeMapper[" + sha + "]";
	}
}
