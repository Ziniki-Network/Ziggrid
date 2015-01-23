package org.ziggrid.model.allocation;

public class NodeUsage {
	final String node;
	final int nthreads;
	final String threadName;

	public NodeUsage(String node, int nthreads, String threadName) {
		this.node = node;
		this.nthreads = nthreads;
		this.threadName = threadName;
	}

	@Override
	public String toString() {
		return node+"x"+nthreads+(threadName != null?("#"+threadName):"");
	}
}
