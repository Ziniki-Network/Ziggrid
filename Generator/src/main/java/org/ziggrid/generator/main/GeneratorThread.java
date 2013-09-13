package org.ziggrid.generator.main;

public class GeneratorThread extends Thread {
	private ControlConnection conn;
	private ZigGenerator generator;
	private boolean done = false;

	public GeneratorThread(ControlConnection conn, ZigGenerator generator) {
		this.conn = conn;
		this.generator = generator;
	}

	@Override
	public void run() {
		done = false;
		generator.resetCounter();
		while (!done && !generator.timeUp()) {
			if (!generator.advanceOneTick())
				break;
			conn.send("{\"tick\":\"" + generator.currentTick() + "\"}");
		}
		synchronized (conn) {
			conn.genThr = null;
		}
	}

	public void pleaseDie() {
		done = true;
	}
}
