package org.ziggrid.driver;

import org.codehaus.jettison.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zincapi.Response;
import org.zincapi.jsonapi.Payload;
import org.zincapi.jsonapi.PayloadItem;

public class ObserverSender implements ObserverListener {
	public final static Logger logger = LoggerFactory.getLogger("Observer");
	private final Response response;

	public ObserverSender(Response response) {
		this.response = response;
	}

	@Override
	public synchronized void deliver(Payload payload) {
		try {
			response.send(payload);
		} catch (JSONException ex) {
			sendError(ex.getMessage());
		}
	}

	public void sendError(String msg) {
		try {
			Payload p = new Payload("errors");
			PayloadItem item = p.newItem();
			item.set("error", msg);
			response.send(p);
		} catch (Exception ex) {
			logger.error("Failed to send error", ex);
		}
	}

	public void close() {
	}
}
