package org.ziggrid.driver.enhancers;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jettison.json.JSONException;
import org.ziggrid.api.StoreableObject;
import org.ziggrid.driver.EnhancementVM;
import org.ziggrid.kvstore.KVTransaction;
import org.ziggrid.kvstore.Sender;
import org.ziggrid.model.Enhancement;
import org.ziggrid.model.EnhancementDefinition;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

import org.zinutils.metrics.CodaHaleMetrics;
import org.zinutils.utils.Crypto;

public class EnhancementDefinitionMapper implements DefinitionMapper {
	private final EnhancementDefinition ed;
	private final String sha;
	public final String to;
	private static final Timer enhancementDMTimer = CodaHaleMetrics.metrics.timer("EnhancementDMTimer");

	public EnhancementDefinitionMapper(EnhancementDefinition ed, String sha) {
		this.ed = ed;
		this.to = ed.to;
		this.sha = sha;
	}

	@Override
	public void process(EnhancementVM evm, Sender sender, KVTransaction tx, StoreableObject prev, StoreableObject item) throws JSONException {
		Context processTimerContext = enhancementDMTimer.time();
		String id = ed.to+"_from_"+item.id();
		Map<String, Object> fields = new HashMap<String, Object>();
		fields.put("_futureId", id);
		for (Entry<String, Enhancement> mf : ed.fields.entrySet()) {
			// Should we try and change this to process StoreableObjects directly?
			Object process = evm.process(mf.getValue(), item.asJsonObject());
			fields.put(mf.getKey(), process);
		}
		sender.queue(tx, sha, Crypto.computeHash(id), fields);
		processTimerContext.stop();
	}
	
	@Override
	public String toString() {
		return "Enhance " + ed.from + " to " + ed.to + "[" + sha + "]";
	}
}
