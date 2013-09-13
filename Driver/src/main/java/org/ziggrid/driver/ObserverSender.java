package org.ziggrid.driver;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.atmosphere.cpr.AsyncIOWriter;
import org.atmosphere.cpr.AtmosphereResponse;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.model.FieldDefinition;
import org.ziggrid.model.Model;
import org.ziggrid.model.ObjectDefinition;
import org.ziggrid.parsing.ErrorHandler;
import org.ziggrid.utils.collections.CollectionUtils;
import org.ziggrid.utils.exceptions.UtilException;
import org.ziggrid.utils.utils.StringUtil;


public class ObserverSender implements Runnable, ObserverListener {
	public final static Logger logger = LoggerFactory.getLogger("Observer");
	private final AtmosphereResponse response;
	private final AsyncIOWriter writer;
	private List<Observer> observers;

	public ObserverSender(AtmosphereResponse response, List<Observer> observers) {
		if (observers == null)
			throw new UtilException("Cannot create ObserverSender without observer");
		this.response = response;
		this.observers = observers;
		this.writer = response.getAsyncIOWriter();
	}

	@Override
	public void run() {
	}
	
	public synchronized void watch(String table, int h, Map<String, Object> options) {
		logger.info("Starting to watch " + table + " handle = " + h + " with options: " + options);
		boolean found = false;
		try {
			for (Observer o : observers)
				found |= o.watch(this, h, table, options);
			if (!found) {
				sendError("there is no table " + table);
				return;
			}
		} catch (Exception ex) {
			sendError(ex.getMessage());
		}
		/*
		try {
			DefaultValue value = proc.interestedIn(this, options);
			Handle handle = value.handle; 
			handles.put(h, handle);
			rhandles.put(handle, h);
			if (value.table != null)
				deliver(handle, value.table);
		} catch (Exception ex) {
			ex.printStackTrace();
			sendError(ex.getMessage());
		}
		*/
	}

	public void unwatch(int hs) {
		logger.info("Stopping watching handle = " + hs);
		for (Observer o : observers)
			o.unwatch(hs);
//		Handle handle = handles.remove(hs);
//		if (handle == null)
//			sendError("There is no handle " + hs);
//		else {
//			rhandles.remove(handle);
//			handle.proc.disinterested(this, handle);
//		}
	}

	/** Upon initialization, send across emberized forms of the (relevant) model definitions
	 */
	public void sendModels(ErrorHandler eh) {
		for (Observer o : observers) {
			Model m = o.getModel();
			for (String docName : m.documents()) {
				for (ObjectDefinition od : m.objects(docName)) {
					try {
						JSONObject model = new JSONObject();
						model.put("modelName", StringUtil.capitalize(od.name));
						JSONObject hash = new JSONObject();
						model.put("model", hash);
						for (FieldDefinition x : od.fields()) {
							hash.put(x.name, attrOf(x.type, x));
						}
						send(model);
					} catch (JSONException ex) {
						logger.error("Error creating models", ex);
					}
				}
				/*
				for (LeaderboardDefinition ld : m.leaderboards(docName)) {
					for (Grouping g : ld.groupings()) {
						try {
							String vn = ld.getViewName(g);
							ObjectDefinition od = m.getModel(eh, vn);
							// First define the entry
							String lbname = StringUtil.capitalize(ld.name) + g.asGroupName();
							String lbentry = lbname + "Entry";
							{
								JSONObject model = new JSONObject();
								model.put("modelName", lbentry);
								JSONObject hash = new JSONObject();
								model.put("model", hash);
								for (FieldDefinition x : od.fields()) {
									hash.put(x.name, attrOf(x.type));
								}
								send(model);
							}
	
							// Then define the board
							{
								JSONObject model = new JSONObject();
								model.put("modelName", lbname);
								JSONObject hash = new JSONObject();
								model.put("model", hash);
								hash.put("entries", new JSONObject(CollectionUtils.map("rel", "hasMany", "name", lbentry)));
								send(model);
							}
	
						} catch (JSONException ex) {
							logger.error("Error creating models", ex);
						}
					}
				}
				*/
			}
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

	@Override
	public synchronized void deliver(Interest i, JSONObject payload) {
		try {
			JSONObject obj = new JSONObject();
			obj.put("deliveryFor", i.handle);
			obj.put("payload", payload);
			send(obj);
		} catch (JSONException ex) {
			sendError(ex.getMessage());
		}
	}

	public void sendError(String msg) {
		try {
			JSONObject obj = new JSONObject();
			obj.put("error", msg);
			send(obj);
		} catch (Exception ex) {
			logger.error("Failed to send error", ex);
		}
	}

	public synchronized void send(JSONObject obj) {
		try {
			logger.info("Sending to client: " + obj.toString());
			writer.write(response, obj.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void close() {
	}
}
