package org.ziggrid.driver;

import org.codehaus.jettison.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zincapi.MulticastResponse;
import org.zincapi.Response;
import org.zincapi.jsonapi.Payload;
import org.zincapi.jsonapi.PayloadItem;

public class ConnectionSender {
	public final static Logger logger = LoggerFactory.getLogger("ConnectionSender");
	private final Response response;

	public ConnectionSender(Response response) {
		this.response = response;
	}

	public void newServer(String type, String endpoint) {
		try {
			Payload p = new Payload("servers");
			PayloadItem item = p.newItem();
			item.set("server", type);
			item.set("endpoint", endpoint);
			logger.info("Sending server payload " + p.asJSONObject());
			response.send(p);
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

	public void send(Payload p) throws JSONException {
		response.send(p);
	}

	// This is only for the multicast one
	public void attachResponse(Response conn) {
		((MulticastResponse)response).attachResponse(conn);
	}
}
