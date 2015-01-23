package org.ziggrid.driver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jettison.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.api.StoreableObject;
import org.ziggrid.kvstore.KVStorageEngine;
import org.ziggrid.kvstore.KVStore;
import org.ziggrid.kvstore.KVTransaction;
import org.ziggrid.kvstore.QueuedItem;
import org.ziggrid.model.NamedEnhancement;
import org.ziggrid.model.SnapshotDefinition;
import org.zinutils.utils.Crypto;
import org.zinutils.utils.StringUtil;

public class ActingSnapshot extends ActingProcessor {
	protected static final Logger logger = LoggerFactory.getLogger("Snapshot");
	private final Set<String> copyFields = new HashSet<String>();
	private final Set<String> reduceFields = new HashSet<String>();
	private final String indType;
	private final String cumType;
	private final SnapshotDefinition sd;
	private final String upToField;

	public ActingSnapshot(String sha, KVStorageEngine store, SnapshotDefinition sd) {
		super(store, "Snapshot");
		this.sd = sd;
		addWatchable(sd.name);
		this.indType = sd.name + "_ind";
		this.cumType = sd.name;
		upToField = sd.upTo.name;
		copyFields.add(upToField);
		for (NamedEnhancement e : sd.group)
			copyFields.add(e.name);
		for (NamedEnhancement e : sd.values)
			reduceFields.add(e.name);
	}

	@Override
	public StoreableObject canYouProvide(Object inTx, String watchable, Map<String, Object> options) {
		JSONArray key = new JSONArray();
		for (NamedEnhancement mf : sd.group) {
			if (!options.containsKey(mf.name))
				return null;
			key.put(options.get(mf.name));
		}
		if (!options.containsKey("upTo"))
			return null;
		Integer upTo = Integer.parseInt((String) options.get("upTo"));
		String myId = cumType + "_" + Crypto.hash(key.toString()) + "/cum/" + StringUtil.digits(upTo, 6);
		return ((KVTransaction)inTx).get(myId);
	}

	// {id:map/snapshot_playerSeasonToDate/974B43A5B642A3DC2F0221207AFA1AB2423A8EFF/86798BA2FB308ED76EB5DD6F3BD326EEAECC7C84/playerTotals_7DFBC6023F375DC76A8355B84E7F175AA642842B/new,
	//  type:snapshot_playerSeasonToDate,
	//  fields:{
	//   _snapshotId=snapshot_playerSeasonToDate_86798BA2FB308ED76EB5DD6F3BD326EEAECC7C84,
	//   _upto=100,
	//   average=0.0, dayOfYear=100, player=biggc001, season=2007
	//  }
	// }

	@Override
	protected void processMessage(KVTransaction tx, KVStore store, QueuedItem qi) {
		String baseid = (String) qi.get("_snapshotId");
		int upto = (Integer) qi.get("_upto");
		
		StoreableObject ind;
		String sid = baseid + "/ind/"+StringUtil.digits(upto, 6);
//		Map<String, Object> oldValues;
		if (tx.contains(sid)) {
			ind = tx.get(sid);
//			oldValues = new HashMap<String, Object>();
		} else {
			ind = new StoreableObject(indType, sid);
//			oldValues = null;
		}
		
		// Update the "individual" object that we will group later
		for (String f : qi.keys()) {
			if (f.startsWith("_") || f.equals("id") || f.equals("ziggridType"))
				continue;
//			if (oldValues != null)
//				oldValues.put(f, ind.get(f));
			ind.set(f, qi.get(f));
		}
		store.write(ind);
		
		// Update (or create) the cumulative item for this object
		StoreableObject cum;
		String cid = baseid + "/cum/"+StringUtil.digits(upto, 6);
		if (tx.contains(cid)) {
			cum = tx.get(cid); 
			cum.set("_reductionCount", ((Integer)cum.get("_reductionCount"))+1);
		} else {
			cum = new StoreableObject(cumType, cid);
			cum.set("_reductionCount", 1);
			for (String f : copyFields)
				cum.set(f, qi.get(f));
			for (String f : reduceFields)
				cum.set(f, 0.0);
		}
		
		int startFrom = (int)sd.startFrom(upto);

		String bfid = baseid + "/ind/"+StringUtil.digits(startFrom, 6);
		List<StoreableObject> priors = tx.getAfter(bfid, baseid+"/ind?");
		priors.add(ind);
		recalculate(store, cum, priors);
		
		// Then update all (existing) "later" objects
		String lastId = baseid + "/cum?";
		for (StoreableObject pc : tx.getAfter(cid+"/", lastId)) {
			pc.set("_reductionCount", ((Integer)cum.get("_reductionCount"))+1);
			recalculate(store, pc, priors);
		}
	}

	public void recalculate(KVStore store, StoreableObject pc, List<StoreableObject> priors) {
		Map<String,Double> values = new HashMap<String,Double>();
		for (String f : reduceFields)
			values.put(f, 0.0);
		int upto = (Integer) pc.get(upToField);
		double denom = include(upto, upto, pc, values);
		for (StoreableObject pr : priors)
			denom += include(upto, (Integer)pr.get(upToField), pr, values);
		if (denom <= 0)
			return;
		for (String f : reduceFields)
			pc.set(f, values.get(f)/denom);
		store.write(pc);
	}

	private double include(int upto, int at, StoreableObject so, Map<String, Double> values) {
		double decay = sd.figureDecay(upto, at);
		if (decay <= 0)
			return 0;
		for (String f : reduceFields) {
			double v = ((Double)so.get(f))*decay;
			values.put(f, values.get(f) + v);
		}
		return decay;
	}
}
