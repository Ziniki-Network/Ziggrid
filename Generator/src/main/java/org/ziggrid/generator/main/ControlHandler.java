package org.ziggrid.generator.main;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.atmosphere.cpr.AtmosphereHandler;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlHandler implements AtmosphereHandler {
	final static Logger logger = LoggerFactory.getLogger("ControlHandler");
	private final Map<HttpSession, ControlConnection> conns = new HashMap<HttpSession, ControlConnection>();

	@Override
	public void onRequest(AtmosphereResource resource) throws IOException {
		AtmosphereRequest request = resource.getRequest();
		HttpSession session = resource.session();
		if (request.getMethod().equalsIgnoreCase("get"))
		{
			// initiating the channel
			resource.suspend();
			ControlConnection conn = new ControlConnection(resource.getResponse());
			conns.put(session, conn);
		}
		else if (request.getMethod().equalsIgnoreCase("post"))
		{
//			AtmosphereResponse response = resource.getResponse();
			ControlConnection conn = conns.get(session);
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
				*/
			} catch (Throwable ex) {
				System.err.println(ex.getMessage());
				conn.send("{\"error\":\"" + ex.getMessage() + "\"}");
			}
		}
	}

	@Override
	public void onStateChange(AtmosphereResourceEvent ev) throws IOException {
		if (ev.getResource().isCancelled()) {
			HttpSession session = ev.getResource().session();
			logger.info("Closing connection for " + session);
			ControlConnection conn = conns.get(session);
			if (conn != null)
				conn.close();
		}
		// This would handle messages from the broadcaster
//		ev.getResource().getResponse().getAsyncIOWriter().write(ev.getResource().getResponse(), "hello");
	}

	@Override
	public void destroy() {

	}

}
