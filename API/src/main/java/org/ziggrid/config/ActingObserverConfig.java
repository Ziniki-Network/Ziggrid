package org.ziggrid.config;

import org.zinutils.collections.PeekableIterator;

public class ActingObserverConfig {
//	private final List<Group> groups = new ArrayList<Group>();
//	private final List<String> types = CollectionUtils.listOf("composite", "correlate", "enhance", "leaderboard", "snapshot", "summary");
	private int liveTime;
	private int fromThread = 0;
	private int toThread = 1;
	private int threadCount = 1;

	public ActingObserverConfig(PeekableIterator<String> argi) {
		while (true) {
			String peek = argi.peek();
			if (peek != null && peek.equals("--threads")) {
				argi.accept();
				fromThread = Integer.parseInt(argi.next());
				toThread = Integer.parseInt(argi.next());
				threadCount = Integer.parseInt(argi.next());
			} else if (peek != null && peek.equals("--live")) {
				argi.accept();
				liveTime = Integer.parseInt(argi.next());
			} else
				break;
		}
	}

	public int stayAliveFor() {
		return liveTime;
	}

	public int getFromThread() {
		return fromThread;
	}

	public int getNumThreads() {
		return toThread;
	}

	public int getThreadCount() {
		return threadCount;
	}
	
	/*
	private void processGroup(String grp, PeekableIterator<String> argi) {
		String peek = argi.peek();
		String table = null;
		int caseNo = -1;
		String from = null;
		String to = null;
				
		if (peek != null && !peek.startsWith("--") && !types.contains(peek)) {
			table = peek;
			argi.accept();
			peek = argi.peek();
			if (peek != null && peek.equals("--case")) {
				argi.accept();
				caseNo = Integer.parseInt(argi.next());
			}
			peek = argi.peek();
			if (peek != null && !peek.startsWith("--") && !types.contains(peek) && peek.length() == 2) {
				// this is a range of queue ids
				from = peek;
				argi.accept();
				to = argi.next();
			}
		}
		groups.add(new Group(grp, table, caseNo, from, to));
	}

	public void checkAllProcessed(Set<Group> processed) {
		if (processed.size() != groups.size()) {
			StringBuilder msg = new StringBuilder("Not all groups were processed:");
			for (Group g : groups)
				if (!processed.contains(g))
					msg.append(" " + g);
			throw new UtilException(msg.toString());
		}
	}

	public Group contains(String type, String creates) {
		if (groups.isEmpty())
			return new Group(type, creates, -1, null, null);
		for (Group g : groups)
			if (g.accepts(type, creates))
				return g;
		return null;
	}

	public Group forSha(String type, String shaFor) {
		if (groups.isEmpty())
			return null;
		for (Group g : groups)
			if (g.accepts(type, shaFor))
				return g;
		return null;
	}

	public class Group {
		private final String type;
		private final String tableOrSha;
		public final int caseNo;
		public final String from;
		public final String to;
	
		public Group(String string, String table, int caseNo, String from, String to) {
			this.type = string;
			this.tableOrSha = table;
			this.caseNo = caseNo;
			this.from = from;
			this.to = to;
		}
	
		public boolean accepts(String defn, String tableSha) {
			if (!defn.equals(type))
				return false;
			return tableOrSha == null || tableOrSha.equals(tableSha);
		}
		
		@Override
		public String toString() {
			return type+"#"+tableOrSha+"["+from+"->"+to+"]";
		}
	}
	*/
}