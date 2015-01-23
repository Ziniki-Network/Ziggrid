package org.ziggrid.main;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class GeneratorThread extends Thread {
	public static final Logger logger = Logger.getLogger("ZigGenerator");
	private GeneratorCommandHandler gh;
	private ZigGenerator generator;
	private boolean done = false;
	private boolean needInit;

	public GeneratorThread(GeneratorCommandHandler gh, ZigGenerator generator) {
		this.gh = gh;
		this.generator = generator;
		this.needInit = true;
	}

	@Override
	public void run() {
		if (needInit) {
			generator.init();
			needInit = false;
		}
			
		done = false;
		generator.resetCounter();
		while (!done && !generator.timeUp()) {
			if (!generator.advanceOneTick())
				break;
			if (gh != null) {
				try {
					JSONObject send = new JSONObject();
					send.put("tick", generator.currentTick());
					gh.send(send);
				} catch (JSONException ex) {
					logger.error(ex);
				} catch (IOException ex) {
					logger.error("Client died; dropping connection and killing thread");
					gh = null;
					break;
				}
			}
		}
		if (gh != null) {
			synchronized (gh.thrs) {
				gh.thrs.remove(this);
				gh.thrs.notify();
			}
		}
		if (generator.timeUp()) {
			logger.info("Time is up; should tell observers to quit");
			// TODO: we should notify direct descendants and so on up the tree
			// Each observer should know how many potential notifications it should get
			// This whole thing is so fake and yet so much trouble ... it annoys me.
			
			// On the other hand, the combining of observers in the configuration
			// means that ifthere is only one ActingObserver, it can do all the work itself
			// and thus will never run into problems.
//			for (ZiggridObserver o : observers)
//				o.close();
		}
	}

	public void pleaseDie() {
		done = true;
	}
}
