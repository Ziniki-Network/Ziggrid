package org.ziggrid.driver;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.api.StoreableObject;
import org.ziggrid.driver.interests.InterestEngine;
import org.ziggrid.kvstore.KVStorageEngine;
import org.ziggrid.kvstore.KVStore;
import org.ziggrid.kvstore.KVTransaction;
import org.ziggrid.kvstore.QueuedItem;
import org.ziggrid.model.Grouping;
import org.ziggrid.model.LeaderboardDefinition;

public class ActingLeader extends ActingProcessor {
	protected static final Logger logger = LoggerFactory.getLogger("Leader");
	private final String sha;
	private final LeaderThreadSync leaderThread;

	public ActingLeader(String sha, KVStorageEngine store, InterestEngine interests, LeaderboardDefinition ld, Grouping grp) {
		super(store, "Leader");
		String watchName = ld.name + grp.asGroupName();
		addWatchable(watchName);
		this.sha = sha;
		leaderThread = new LeaderThreadSync(store, interests, watchName, grp, sha, ld.top, ld.ascending);
	}

	@Override
	public StoreableObject canYouProvide(Object inTx, String watchable, Map<String, Object> options) {
		return leaderThread.canYouProvide(inTx, watchable, options);
	}

	@Override
	protected void processMessage(KVTransaction tx, KVStore store, QueuedItem qi) {
		String sorts = (String) qi.get("sorts");
		String value = (String) qi.get("value");
		String group = (String) qi.get("group");
		String vid = "leaderboardv/"+sha+group+"/"+value;
		int when = (Integer)qi.get("when");
		int pw = -1;
		StoreableObject meta;
		String oldSorts = null;
		if (tx.contains(vid)) {
			// we have seen this value before; get the relevant metadata
			meta = tx.get(vid);
			pw = (Integer)meta.get("when");
			oldSorts = (String)meta.get("sorts");
		} else {
			meta = new StoreableObject(null, vid);
		}
		// If we are receiving out of date info, don't do anything more ...
		if (when < pw)
			return;
		
		// OK, update it
		meta.set("when", when);
		meta.set("sorts", sorts);
		tx.put(vid, meta);
		
		leaderThread.update(group, value, sorts, oldSorts);
	}

	@Override
	public String toString() {
		return "ActingLeader[" + watchables() + "]";
	}
}
