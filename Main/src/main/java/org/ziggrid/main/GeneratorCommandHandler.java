package org.ziggrid.main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.driver.ServerStore;
import org.ziggrid.exceptions.ZiggridException;
import org.zincapi.HandleRequest;
import org.zincapi.ResourceHandler;
import org.zincapi.Response;
import org.zinutils.sync.SyncUtils;

public class GeneratorCommandHandler implements ResourceHandler {
	final static Logger logger = LoggerFactory.getLogger("ControlHandler");
	final ServerStore serverStore;
	private List<ZigGenerator> generators;
	final List<GeneratorThread> thrs = new ArrayList<GeneratorThread>();

	public GeneratorCommandHandler(ServerStore serverStore) {
		this.serverStore = serverStore;
	}

	public void setGenerators(List<ZigGenerator> generators) {
		this.generators = generators;
	}

	@Override
	public void handle(HandleRequest hr, Response response) throws Exception {
		if (hr.isInvoke()) {
			String resource = hr.getResource();
			if (resource.equals("generator/start"))
				start();
			else if (resource.equals("generator/stop"))
				stop();
			else
				logger.error("Generator cannot handle " + hr);
		} else
			logger.error("Generator cannot handle " + hr);
	}
	
	public void start() {
		logger.info("start");
		if (!thrs.isEmpty())
			throw new ZiggridException("Cannot start - already running");
		for (ZigGenerator zg : generators) {
			GeneratorThread genThr = new GeneratorThread(this, zg);
			genThr.start();
			thrs.add(genThr);
		}
	}
	
	public void stop() {
		logger.info("stop");
		if (thrs.isEmpty())
			throw new ZiggridException("Cannot stop - not running");
		synchronized (thrs) {
			for (GeneratorThread genThr : thrs)
				genThr.pleaseDie();
			while (!thrs.isEmpty())
				SyncUtils.waitFor(thrs, 100);
		}
	}

	public void delay(int d) {
		for (ZigGenerator zg : generators)
			zg.delay = d;
	}

	public void send(JSONObject msg) throws IOException {
		// TODO: use a multicast response for this
		
	}

	/*
	public void onRequest(AtmosphereResource resource) throws IOException {
		AtmosphereRequest request = resource.getRequest();
		HttpSession session = resource.session();
		if (request.getMethod().equalsIgnoreCase("get"))
		{
			// initiating the channel
			resource.suspend();
			GeneratorConnection conn = new GeneratorConnection(generators, resource.getResponse());
			conns.put(session, conn);
		}
		else if (request.getMethod().equalsIgnoreCase("post"))
		{
//			AtmosphereResponse response = resource.getResponse();
			GeneratorConnection conn = conns.get(session);
			String input = request.getReader().readLine().trim();
			logger.info("Handling input " + input);
			try {
				JSONObject req = new JSONObject(input);
				if (!req.has("action")) {
					conn.send("{\"error\":\"Cannot process generator command without 'action''\"}");
					return;
				}
				String action = req.getString("action");
				if (action.equals("start"))
					conn.start();
				else if (action.equals("stop"))
					conn.stop();
				else if (action.equals("delay")) {
					int d = req.getInt("size");
					conn.delay(d);
				}
				else
					conn.send("{\"error\":\"Cannot handle action '" + action + "'\"}");
				/*
				if (req.has("watch")) {
					Map<String, String> options = new HashMap<String, String>();
					@SuppressWarnings("rawtypes")
					Iterator keys = req.keys();
					while (keys.hasNext()) {
						String k = (String) keys.next();
						if (k.equals("watch") || k.equals("unique"))
							continue;
						options.put(k, req.getString(k));
					}
					sender.watch(req.getString("watch"), req.getInt("unique"), options);
				} else if (req.has("unwatch")) {
					// Do we need to do something here?
				} else {
					sender.send("{\"error\":\"no recognized command\"}");
				}
				* /
			} catch (Throwable ex) {
				System.err.println(ex.getMessage());
				conn.send("{\"error\":\"" + ex.getMessage() + "\"}");
			}
		}
	}
	*/
}
