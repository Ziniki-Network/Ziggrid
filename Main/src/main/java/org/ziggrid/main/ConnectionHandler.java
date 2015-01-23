package org.ziggrid.main;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.driver.ConnectionSender;
import org.ziggrid.driver.ServerInfo;
import org.ziggrid.driver.ServerInfo.ServerStatus;
import org.ziggrid.driver.ServerStore;
import org.ziggrid.driver.interests.InterestEngine;
import org.ziggrid.model.FieldDefinition;
import org.ziggrid.model.Model;
import org.ziggrid.model.ObjectDefinition;
import org.zincapi.HandleRequest;
import org.zincapi.ResourceHandler;
import org.zincapi.Response;
import org.zincapi.Zinc;
import org.zincapi.jsonapi.Payload;
import org.zincapi.jsonapi.PayloadItem;
import org.zinutils.collections.CollectionUtils;
import org.zinutils.sync.SyncUtils;
import org.zinutils.utils.StringUtil;

public class ConnectionHandler implements ResourceHandler {
	final static Logger logger = LoggerFactory.getLogger("UpdateHandler");
	private final ConnectionSender serverResponse;
	private final Map<String, ServerInfo> knownServers = new TreeMap<String, ServerInfo>();
	final ServerStore serverStore;
	private InterestEngine interests;
	private NotifyClientsThread thr;

	public ConnectionHandler(Zinc zinc, ServerStore serverStore) {
		this.serverStore = serverStore;
		serverResponse = new ConnectionSender(zinc.getMulticastResponse("servers"));
		thr = new NotifyClientsThread();
		thr.setDaemon(true);
		thr.setName("NotifyClientsConnection");
		thr.start();
	}

	public void setInterestEngine(InterestEngine interests) {
		this.interests = interests;
	}

	@Override
	public void handle(HandleRequest hr, Response response) throws Exception {
		if (hr.isSubscribe()) {
			ConnectionSender sender = new ConnectionSender(response);
			if (hr.getResource().equals("servers")) {
				synchronized (knownServers) {
					serverResponse.attachResponse(response);
					sendServers(sender);
				}
			} else if (hr.getResource().equals("models"))
				sendModels(sender); 
			else if (hr.getResource().equals("watchables"))
				sendWatchables(sender);
			else {
				logger.error("Invalid request: " + hr);
			}
		} else {
			logger.error("Invalid request: " + hr);
		}
	}

	public void sendServers(ConnectionSender sender) throws JSONException {
		synchronized (knownServers) {
			for (ServerInfo s : knownServers.values()) {
				sender.newServer(s.type, s.addr);
			}
		}
	}

	/** Upon initialization, send across emberized forms of the (relevant) model definitions
	 */
	public void sendModels(ConnectionSender sender) {
		Payload p = new Payload("models");
		Model m = interests.getModel();
		for (String docName : m.documents()) {
			for (ObjectDefinition od : m.objects(docName)) {
				PayloadItem item = p.newItem();
				try {
					item.set("modelName", StringUtil.capitalize(od.name));
					JSONObject hash = new JSONObject();
					item.set("model", hash);
					for (FieldDefinition x : od.fields()) {
						hash.put(x.name, attrOf(x.type, x));
					}
				} catch (JSONException ex) {
					logger.error("Error creating models", ex);
				}
			}
		}
		try {
			sender.send(p);
		} catch (JSONException ex) {
			logger.error("Error creating models", ex);
		}
	}
	
	public void sendWatchables(ConnectionSender sender) {
		try {
			Payload p = new Payload("watchables");
			for (String s : interests.getWatchables()) {
				PayloadItem item = p.newItem();
				item.set("name", s);
			}
			sender.send(p);
		} catch (Exception ex) {
			sender.sendError(ex.getMessage());
		}
	}

	private JSONObject attrOf(String type, FieldDefinition x) {
		if (type.equals("string") || type.equals("number") || type.equals("boolean"))
			return new JSONObject(CollectionUtils.map("rel", "attr", "name", type));
		else if (type.equals("list"))
			return new JSONObject(CollectionUtils.map("rel", "hasMany", "name", x.param(0)));
		else
			return new JSONObject(CollectionUtils.map("rel", "unknown"));
	}

	public class NotifyClientsThread extends Thread {
		public void run() {
			while (true) {
				Collection<ServerInfo> list = serverStore.list();
				try {
					synchronized (knownServers) {
						Set<String> notPresent = new TreeSet<String>();
						notPresent.addAll(knownServers.keySet());
						for (ServerInfo s : list) {
							ServerStatus stat;
							if (knownServers.containsKey(s.key)) {
								stat = knownServers.get(s.key).update(s);
							} else {
								stat = ServerStatus.CREATED;
								knownServers.put(s.key, s);
							}
							if (stat == ServerStatus.CREATED) {
								serverResponse.newServer(s.type, s.addr);
							}
							if (stat == ServerStatus.EXPIRED) {
								serverStore.delete(s);
							} else
								notPresent.remove(s.key);
						}
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				SyncUtils.sleep(3500);
			}
		}
	}

}
