package org.ziggrid.main;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.ziggrid.config.CouchStorageConfig;
import org.ziggrid.config.FoundationStorageConfig;
import org.ziggrid.config.MemoryStorageConfig;
import org.ziggrid.config.StorageConfig;
import org.ziggrid.driver.FoundationServerStore;
import org.ziggrid.driver.MemoryServerStore;
import org.ziggrid.driver.ServerStore;
import org.ziggrid.driver.interests.InterestEngine;
import org.ziggrid.exceptions.ZiggridException;
import org.ziggrid.main.WebConfig.Transpiler;
import org.zincapi.Zinc;
import org.zincapi.inline.server.ZincServlet;
import org.zinutils.collections.CollectionUtils;
import org.zinutils.http.ISServletDefn;
import org.zinutils.http.InlineServer;
import org.zinutils.http.NotifyOnServerReady;
import org.zinutils.http.js.TranspilerAsset;
import org.zinutils.serialization.Endpoint;

public class WebServer implements Runnable {
//	private static final Logger logger = LoggerFactory.getLogger("WebServer");
	private InlineServer server;
	private WebConfig config;
	private Zinc zinc;
	private ServerStore serverStore;

	public WebServer(WebConfig c) {
		this.config = c;
		configure(c.getStaticDirs(), c.getStaticResources(), c.getPort());
	}
	
	@Deprecated
	public WebServer(final String couchUrl, final String bucket, Object... args) {
		if (args == null || args.length < 2)
			throw new ZiggridException("Usage: WebServer static-content port");
		File staticRoot = new File((String)args[0]);
		if (!staticRoot.isDirectory())
			throw new ZiggridException("No such directory: " + staticRoot);
		int port = Integer.parseInt((String) args[1]);
		configure(CollectionUtils.listOf(staticRoot), new ArrayList<String>(), port);
	}
	
	public void configure(List<File> sdirs, List<String> sresources, int port) {
		server = new InlineServer(port, "org.zincapi.inline.server.ZincServlet");
		if (config.transpilerModuleDir != null)
			server.configureTranspiler(config.transpilerModuleDir);
		for (Transpiler tr : config.transpilers) {
			TranspilerAsset appjs = server.transpile(tr.path);
			if (tr.style.equals("--js")) {
				appjs.handleJS(tr.prefix(), tr.from, tr.tmp);
			} else if (tr.style.equals("--ember")) {
				appjs.handleEmberTemplates(tr.prefix(), tr.from, tr.tmp);
			}
		}
		ISServletDefn servlet = server.getBaseServlet();
		servlet.initParam("org.atmosphere.cpr.sessionSupport", "true");
//		servlet.initParam("org.zincapi.server.init", "org.ziggrid.main.ZincInitializer");
		servlet.setServletPath("/ziggrid");
		for (File f: sdirs)
			server.addStaticDir(f);
		for (String s: sresources)
			server.addStaticResource(s);
		StorageConfig storage = config.getStorage();
		if (storage instanceof CouchStorageConfig) {
			CouchStorageConfig cs = (CouchStorageConfig)storage;
//			serverStore = new CouchServerStore(cs.dbUrl, cs.bucket);
		} else if (storage instanceof MemoryStorageConfig)
			serverStore = new MemoryServerStore();
		else if (storage instanceof FoundationStorageConfig) {
			String cf = ((FoundationStorageConfig)storage).clusterFile();
			serverStore = new FoundationServerStore(cf);
		} else
			throw new ZiggridException("Cannot create a server store");
	}
	
	public void addConnectionServlet(final InterestEngine interests) {
		server.notify(new NotifyOnServerReady() {
			public void serverReady(InlineServer inlineServer, String scheme, Endpoint addr) {
				if (zinc == null)
					zinc = ((ZincServlet)inlineServer.getBaseServlet().getImpl()).getZinc();
				ConnectionHandler ch = new ConnectionHandler(zinc, serverStore);
				zinc.handleResource("models", ch);
				zinc.handleResource("servers", ch);
//				ConnectionHandler ch = ((ConnectionServlet)server.servletFor("/ziggrid/connmgr").getImpl()).connHandler;
				ch.setInterestEngine(interests);
			}
		});
//		ISServletDefn servlet = newServlet("/ziggrid", "/connmgr", "org.ziggrid.main.ConnectionServlet");
//		addStorageParams(servlet);
	}

	public void addGeneratorServlet(final List<ZigGenerator> generators) {
		server.notify(new NotifyOnServerReady() {
			public void serverReady(InlineServer inlineServer, String scheme, Endpoint addr) {
				if (zinc == null)
					zinc = ((ZincServlet)inlineServer.getBaseServlet().getImpl()).getZinc();
				GeneratorCommandHandler gh = new GeneratorCommandHandler(serverStore);
				zinc.handleResource("generator/start", gh);
				zinc.handleResource("generator/stop", gh);
				zinc.handleResource("generator/setDelay", gh);
				gh.serverStore.register("generator", addr.toString());
				gh.setGenerators(generators);
				// old couchbase version:
				//					try {
//						JSONObject obj = new JSONObject();
//						obj.put("webserver", "generator");
//						obj.put("endpoint", endpoint);
//						analyticStore.recordServer("webServer/" + endpoint, obj);
//					} catch (JSONException ex) {
//						throw UtilException.wrap(ex);
//					}
			}
		});

//		ISServletDefn servlet = newServlet("/ziggrid", "/generator", "org.ziggrid.main.GeneratorServlet");
//		addStorageParams(servlet);
	}

	public void addObserverServlet(final InterestEngine interests) {
		server.notify(new NotifyOnServerReady() {
			public void serverReady(InlineServer inlineServer, String scheme, Endpoint addr) {
				if (zinc == null)
					zinc = ((ZincServlet)inlineServer.getBaseServlet().getImpl()).getZinc();
				String endpoint = addr.toString();
				UpdateHandler uh = new UpdateHandler(serverStore);
				zinc.handleResource("watch/GameDate", uh);
				zinc.handleResource("watch/WinLoss", uh);
				zinc.handleResource("watch/Profile", uh);
				zinc.handleResource("watch/Leaderboard_homeruns_groupedBy_season", uh);
				zinc.handleResource("watch/Leaderboard_production_groupedBy_season", uh);
				zinc.handleResource("watch/Leaderboard_average_groupedBy_season", uh);
				uh.serverStore.register("ziggrid", endpoint);
				uh.setInterestEngine(interests);
//				This used to look like this for Couchbase:
//				((UpdateServlet)server.getBaseServlet().getImpl()).updateHandler.serverStore.register("webServer/"+endpoint, "{\"webserver\":\"ziggrid\",\"endpoint\":\"" + endpoint + "\"}");
			}
		});

//		ISServletDefn servlet = newServlet("/ziggrid", "/observer", "org.ziggrid.main.UpdateServlet");
//		addStorageParams(servlet);
	}

	public void addStorageParams(ISServletDefn servlet) {
		servlet.initParam("org.ziggrid.asyncSupportClass", "org.ziggrid.utils.http.ws.AsyncProcessor");
		StorageConfig storage = config.getStorage();
		if (storage instanceof MemoryStorageConfig)
			servlet.initParam("org.ziggrid.memoryStore", "true"); // could be parameter if useful
		else if (storage instanceof FoundationStorageConfig) {
			String cf = ((FoundationStorageConfig)storage).clusterFile();
			servlet.initParam("org.ziggrid.foundationStore", cf != null?cf:"");
		}
	}

	public ISServletDefn newServlet(String cxt, String path, String clz) {
		return server.addServlet(cxt, path, clz);
	}

	@Override
	public void run() {
		server.run();
	}

	/*
	public CouchbaseClient openCouch(String couchUrl, String bucket) {
		try {
			URI server = new URI(couchUrl+"pools");
			List<URI> serverList = new ArrayList<URI>();
			serverList.add(server);
			CouchbaseConnectionFactoryBuilder builder = new CouchbaseConnectionFactoryBuilder();
			builder.setOpTimeout(30000);
			builder.setTimeoutExceptionThreshold(30000);
			CouchbaseConnectionFactory ccf = builder.buildCouchbaseConnection(serverList, bucket, "");
			return new CouchbaseClient(ccf);
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}
	*/
}
