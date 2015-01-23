package org.ziggrid.main;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.driver.ObserverSender;
import org.ziggrid.driver.ServerStore;
import org.ziggrid.driver.interests.InterestEngine;
import org.zincapi.HandleRequest;
import org.zincapi.ResourceHandler;
import org.zincapi.Response;

public class UpdateHandler implements ResourceHandler {
	final static Logger logger = LoggerFactory.getLogger("UpdateHandler");
	final ServerStore serverStore;
	private InterestEngine interests;

	public UpdateHandler(ServerStore serverStore) {
		this.serverStore = serverStore;
	}

	public void setInterestEngine(InterestEngine interests) {
		this.interests = interests;
	}

	@Override
	public void handle(HandleRequest hr, Response response) throws Exception {
		if (hr.isSubscribe()) {
			ObserverSender sender = new ObserverSender(response);
			String r = hr.getResource(); // todo : get the portion we actually want instead of this code
			r = r.replace("watch/", "").toLowerCase();
			Map<String, Object> options = hr.options();
			interests.watch(sender, r, options);
		}
	}
	// TODO: handle break
	// interests.unwatch(sender, req.getInt("unwatch"));
}
