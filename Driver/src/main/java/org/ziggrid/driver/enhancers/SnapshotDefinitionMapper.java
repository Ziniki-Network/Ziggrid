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
import org.ziggrid.model.NamedEnhancement;
import org.ziggrid.model.SnapshotDefinition;
import org.zinutils.metrics.CodaHaleMetrics;
import org.zinutils.utils.Crypto;
import org.zinutils.utils.StringUtil;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

public class SnapshotDefinitionMapper implements DefinitionMapper {
	public final String myName;
	private final String sha;
	private final List<NamedEnhancement> groupBy;
	private final NamedEnhancement upTo;
	private final List<NamedEnhancement> fields;
	private static final Timer snapshotDMTimer = CodaHaleMetrics.metrics.timer("SnapshotDMTimer");

	public SnapshotDefinitionMapper(SnapshotDefinition sd, String sha) {
		this.sha = sha;
		myName = sd.name;
		groupBy = sd.group;
		upTo = sd.upTo;
		fields = sd.values;
	}

	@Override
	public void process(EnhancementVM evm, Sender sender, KVTransaction tx, StoreableObject prev, StoreableObject item) throws JSONException {
		Context processTimerContext = snapshotDMTimer.time();
		JSONObject json = item.asJsonObject();
		byte[] sid = snapshotId(evm, json);
		Object uptoV = evm.process(upTo.enh, json);

		// Now (over) write new version while writing out old version
		Map<String,Object> mapped = new HashMap<String, Object>();
		mapped.put("_snapshotId", myName + "_" + StringUtil.hex(sid));
		mapped.put("_upto", uptoV);
		mapped.put(upTo.name, uptoV);
		for (NamedEnhancement mf : groupBy) {
			mapped.put(mf.name, evm.process(mf.enh, json));
		}
		for (NamedEnhancement ne : fields) {
			mapped.put(ne.name, evm.process(ne.enh, json));
		}
		sender.queue(tx, sha, sid, mapped);
		processTimerContext.stop();
	}
	
	private byte[] snapshotId(EnhancementVM evm, JSONObject json) throws JSONException {
		JSONArray key = new JSONArray();
		for (NamedEnhancement mf : groupBy) {
			key.put(evm.process(mf.enh, json));
		}
		return Crypto.computeHash(key.toString());
	}
	
	@Override
	public String toString() {
		return "SnapshotMapper[" + sha + "]";
	}
}
