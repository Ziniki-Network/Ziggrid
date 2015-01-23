package org.ziggrid.main;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.zinutils.collections.CollectionUtils;

public class ZiggridMemoryDemo extends Ziggrid {

	public static void main(String[] args) {
		Map<String, Object> opts = CollectionUtils.map(
				"--storage", CollectionUtils.listOf(new String[] { "--storage", "memory" }),
				"--model", CollectionUtils.listOf(new String[] { "--model", "resource:buckets", "ziggrid-baseball" }),
				"--web", CollectionUtils.listOf(new String[] { "--web", "--resource", "/beane-counter-ui" }),
				"--observer", CollectionUtils.listOf(new String[] { "--observer", "--threads", "0", "100", "100" }),
				"--generator", CollectionUtils.listOf(new String[] { "--generator", "resource:baseball.xml", "--group", "0", "1" }));
		if (args.length > 0) {
			String who = args[0];
			if (!opts.containsKey(who)) {
				System.err.println("There is no valid option " + who);
				System.exit(1);
			}
			List<String> tmp = null;
			for (String s : args) {
				if (opts.containsKey(s)) {
					if (tmp != null)
						opts.put(who, tmp);
					who = s;
					tmp = new ArrayList<String>();
				}
				tmp.add(s);
			}
			opts.put(who, tmp);
		}
		Ziggrid.main(combine(opts.get("--storage"), opts.get("--model"), opts.get("--web"), opts.get("--observer"), opts.get("--generator")));
	}

	@SuppressWarnings("unchecked")
	private static String[] combine(Object... args) {
		List<String> ret = new ArrayList<String>();
		for (Object a : args)
			ret.addAll((Collection<? extends String>) a);
		return ret.toArray(new String[ret.size()]);
	}
}
