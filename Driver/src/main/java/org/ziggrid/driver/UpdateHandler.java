package org.ziggrid.driver;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpSession;

import org.apache.commons.httpclient.HttpClient;
import org.atmosphere.cpr.AtmosphereHandler;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.couchbase.CouchDocument;
import org.ziggrid.couchbase.CouchView;
import org.ziggrid.parsing.ErrorHandler;
import org.ziggrid.utils.collections.CollectionUtils;
import org.ziggrid.utils.exceptions.UtilException;
import org.ziggrid.utils.jsgen.JSCompiler;
import org.ziggrid.utils.sync.SyncUtils;

import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.CouchbaseConnectionFactory;
import com.couchbase.client.CouchbaseConnectionFactoryBuilder;
import com.couchbase.client.protocol.views.InvalidViewException;
import com.couchbase.client.protocol.views.Query;
import com.couchbase.client.protocol.views.Stale;
import com.couchbase.client.protocol.views.View;
import com.couchbase.client.protocol.views.ViewRow;

public class UpdateHandler implements AtmosphereHandler {
	final static Logger logger = LoggerFactory.getLogger("UpdateHandler");
	private final Map<HttpSession, ObserverSender> conns = new HashMap<HttpSession, ObserverSender>();
	private final String couchUrl;
	private final String bucket;
	private CouchbaseClient couch;
	private View nodesView;

	public UpdateHandler(String couchUrl, String bucket) {
		this.couchUrl = couchUrl;
		this.bucket = bucket;
		openCouch(couchUrl, bucket);
	}

	@Override
	public void onRequest(AtmosphereResource resource) throws IOException {
		AtmosphereRequest request = resource.getRequest();
		HttpSession session = resource.session();
		if (request.getMethod().equalsIgnoreCase("get"))
		{
			// initiating the channel
			resource.suspend();
			ObserverSender sender = new ObserverSender(resource.getResponse(), Ziggrid.observers);
			conns.put(session, sender);
		}
		else if (request.getMethod().equalsIgnoreCase("post"))
		{
//			AtmosphereResponse response = resource.getResponse();
			ObserverSender sender = conns.get(session);
			String input = request.getReader().readLine().trim();
			logger.info("Handling input " + input);
			try {
				JSONObject req = new JSONObject(input);
				if (req.has("watch")) {
					Map<String, Object> options = new HashMap<String, Object>();
					@SuppressWarnings("rawtypes")
					Iterator keys = req.keys();
					while (keys.hasNext()) {
						String k = (String) keys.next();
						if (k.equals("watch") || k.equals("unique"))
							continue;
						options.put(k, req.get(k));
					}
					sender.watch(req.getString("watch"), req.getInt("unique"), options);
				} else if (req.has("unwatch")) {
					sender.unwatch(req.getInt("unwatch"));
				} else if (req.has("action")) {
					String action = req.getString("action");
					handleAction(sender, req, action);
				} else {
					sender.sendError("no command identified");
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				sender.sendError(ex.getMessage());
			}
		}
	}

	private void handleAction(ObserverSender sender, JSONObject req, String action) throws JSONException {
		if (action.equals("init")) {
			Query q = new Query();
			q.setReduce(false);
			q.setStale(Stale.FALSE);
			for (ViewRow r : couch.query(nodesView, q)) {
				JSONObject server = new JSONObject();
				server.put("server", r.getKey());
				server.put("endpoint", r.getValue());
				sender.send(server);
			}
			ErrorHandler eh = new ErrorHandler();
			sender.sendModels(eh);
			sender.send(new JSONObject(CollectionUtils.map("status", "initdone")));
			eh.displayErrors();
		} else
			sender.sendError("there is no action " + action);
	}

	@Override
	public void onStateChange(AtmosphereResourceEvent ev) throws IOException {
		if (ev.getResource().isCancelled()) {
			HttpSession session = ev.getResource().session();
			logger.info("Closing connection for " + session);
			ObserverSender sender = conns.get(session);
			if (sender != null)
				sender.close();
		}
		// This would handle messages from the broadcaster
//		ev.getResource().getResponse().getAsyncIOWriter().write(ev.getResource().getResponse(), "hello");
	}

	public void openCouch(String couchUrl, String bucket) {
		try {
			logger.error("UpdateHandler opening private couchbase connection");
			URI server = new URI(couchUrl+"pools");
			List<URI> serverList = new ArrayList<URI>();
			serverList.add(server);
			CouchbaseConnectionFactoryBuilder builder = new CouchbaseConnectionFactoryBuilder();
			builder.setOpTimeout(30000);
			builder.setTimeoutExceptionThreshold(30000);
			CouchbaseConnectionFactory ccf = builder.buildCouchbaseConnection(serverList, bucket, "");
			couch = new CouchbaseClient(ccf);
			for (int i=0;i<4;i++) {
				try {
					nodesView = couch.getView("ziggrid", "nodes");
				} catch (InvalidViewException ex) {
					// if it's not there, put it there
					loadZiggridDesignDoc();
					SyncUtils.sleep(100);
				}
			}
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	private void loadZiggridDesignDoc() {
		CouchDocument doc = new CouchDocument("ziggrid");
		CouchView view = doc.create("nodes");
		new JSCompiler(view.needMap().getBlock()) {
			public void compile() {
				ifNEq(var("meta").member("type"), string("json")).yes.returnVoid();
				ifFalsy(var("doc").member("webserver")).yes.returnVoid();
				voidFunction("emit", var("doc").member("webserver"), var("doc").member("endpoint"));
			}
		};
		try {
			HttpClient cli = new HttpClient();
			doc.writeToCouch(cli, couchUrl + "couchBase/" + bucket + "/");
		} catch (Exception ex) {
			logger.error("Failed to write " + doc.getName() + " to " + couchUrl, ex);
			SyncUtils.sleep(250);
		}
	}


	@Override
	public void destroy() {
		logger.error("Update handler shutting down private couchbase connection");
		couch.shutdown(1, TimeUnit.MINUTES);
	}

}
