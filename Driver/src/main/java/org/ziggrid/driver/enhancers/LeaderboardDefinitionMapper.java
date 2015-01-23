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
import org.ziggrid.model.Grouping;
import org.ziggrid.model.LeaderboardDefinition;
import org.ziggrid.model.NamedEnhancement;
import org.zinutils.metrics.CodaHaleMetrics;
import org.zinutils.utils.Crypto;
import org.zinutils.utils.StringUtil;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

public class LeaderboardDefinitionMapper implements DefinitionMapper {
	private final LeaderboardDefinition ld;
	private Grouping grouping;
	private final List<NamedEnhancement> filters;
	private final String sha;
	public final String name;
	private static final Timer leaderboardDMTimer = CodaHaleMetrics.metrics.timer("LeaderboardDMTimer");

	public LeaderboardDefinitionMapper(LeaderboardDefinition ld, Grouping g, String sha) {
		this.ld = ld;
		this.filters = ld.filters;
		this.grouping = g;
		this.sha = sha;
		this.name = ld.name + g.asGroupName();
	}

	@Override
	public void process(EnhancementVM evm, Sender sender, KVTransaction tx, StoreableObject prev, StoreableObject item) throws JSONException {
		Context processTimerContext = leaderboardDMTimer.time();
		JSONObject iJSON = item.asJsonObject();
		
		for (NamedEnhancement f : filters) {
			Object foo = evm.process(f.enh, iJSON);
			if (foo == null || (foo instanceof Boolean && !(Boolean)foo) || (foo instanceof Integer && ((Integer)foo) == 0))
				return;
		}
		int when = (Integer) item.get("_reductionCount");
		Map<String,Object> mapped = new HashMap<String, Object>();
		StringBuilder key = new StringBuilder(sha);
		{
			StringBuilder groupB = new StringBuilder();
			for (String f : grouping.fields)
				groupB.append("/"+item.get(f));
			String grp = groupB.toString();
			mapped.put("group", grp);
			key.append(grp);
		}
		{
			StringBuilder sortB = new StringBuilder();
			for (NamedEnhancement s : ld.sorts)
				sortB.append("/" + alphaSort(evm.process(s.enh, iJSON)));
			mapped.put("sorts", sortB.toString());
		}
		{
			StringBuilder valueB = new StringBuilder();
			for (String v : ld.values)
				valueB.append("/"+item.get(v));
			mapped.put("value", valueB.substring(1));
			key.append(valueB.toString());
		}
		mapped.put("when", when);
		
		sender.queue(tx, sha, Crypto.computeHash(key.toString()), mapped);
		processTimerContext.stop();
	}
	
	private String alphaSort(Object process) {
		if (process instanceof String)
			return (String) process;
		else if (process instanceof Integer)
			return StringUtil.digits((Integer)process, 10);
		else if (process instanceof Double)
			return StringUtil.digits((Double)process, 5, 5);
		return null;
	}

	@Override
	public String toString() {
		return "LeaderMapper[" + sha + "]";
	}
}
