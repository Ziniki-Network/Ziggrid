package org.ziggrid.generator.out.ws;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.atmosphere.cpr.AtmosphereHandler;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.ziggrid.generator.main.ZigGenerator;

public class AnalyticHandler implements AtmosphereHandler {
	private final Map<HttpSession, WebSocketSender> conns = new HashMap<HttpSession, WebSocketSender>();
	
	@Override
	public void onRequest(AtmosphereResource resource) throws IOException {
		AtmosphereRequest request = resource.getRequest();
		HttpSession session = resource.session();
		ZigGenerator.logger.info("Received " + request.getMethod() + " from session " + session);
		if (request.getMethod().equalsIgnoreCase("get"))
		{
			// initiating the channel
			resource.suspend();
			WebSocketSender store = new WebSocketSender(resource.getResponse());
			conns.put(session, store);
		} else if (request.getMethod().equalsIgnoreCase("post")) {
			WebSocketSender sender = conns.get(session);
			if (sender == null)
				ZigGenerator.logger.severe("There is no generator for session");
			else {
				ZigGenerator.logger.info("Starting generator");
				final ZigGenerator tmp = new ZigGenerator(AnalyticServer.realtime, AnalyticServer.delay, 0, AnalyticServer.config, sender);
				tmp.setEndTime(-1);
				new Thread() {
					public void run() {
						tmp.doAll();
					}
				}.start();
				sender.setGenerator(tmp);
			}
		}
	}

	@Override
	public void onStateChange(AtmosphereResourceEvent ev) throws IOException {
		if (ev.getResource().isCancelled()) {
			HttpSession session = ev.getResource().session();
			ZigGenerator.logger.info("Closing connection for " + session);
			WebSocketSender sender = conns.get(session);
			if (sender == null)
				ZigGenerator.logger.severe("There is no generator for session");
			else {
				ZigGenerator.logger.info("Stopping generator");
				sender.close();
			}
		}
		// This would handle messages from the broadcaster
//		ev.getResource().getResponse().getAsyncIOWriter().write(ev.getResource().getResponse(), "hello");
	}

	@Override
	public void destroy() {
		System.out.println("Destroying AnalyticHandler");
	}
}
