package org.ziggrid.driver;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.exceptions.ZiggridException;
import org.ziggrid.utils.exceptions.UtilException;
import org.ziggrid.utils.http.GPServletDefn;
import org.ziggrid.utils.http.InlineServer;
import org.ziggrid.utils.http.NotifyOnServerReady;
import org.ziggrid.utils.serialization.Endpoint;

import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.CouchbaseConnectionFactory;
import com.couchbase.client.CouchbaseConnectionFactoryBuilder;

public class WebServer implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger("WebServer");
	private InlineServer server;

	public WebServer(File staticContent, int port) {
		configure(staticContent, port);
	}
	
	public WebServer(final String couchUrl, final String bucket, Object... args) {
		if (args == null || args.length < 2)
			throw new ZiggridException("Usage: WebServer static-content port");
		File staticRoot = new File((String)args[0]);
		if (!staticRoot.isDirectory())
			throw new ZiggridException("No such directory: " + staticRoot);
		int port = Integer.parseInt((String) args[1]);
		configure(staticRoot, port);
		server.getBaseServlet().initParam("org.ziggrid.couchUrl", couchUrl);
		server.getBaseServlet().initParam("org.ziggrid.bucket", bucket);
		server.notify(new NotifyOnServerReady() {
			public void serverReady(InlineServer inlineServer, Endpoint addr) {
				logger.error("WebServer is opening a private couchbase connection");
				CouchbaseClient conn = openCouch(couchUrl, bucket);
				String endpoint = addr.toString();
				conn.set("webServer/"+endpoint, 0, "{\"webserver\":\"ziggrid\",\"endpoint\":\"" + endpoint + "\"}");
				logger.error("WebServer is closing its private couchbase connection");
				conn.shutdown(1, TimeUnit.MINUTES);
			}
		});
	}
	
	public void configure(File staticContent, int port) {
		server = new InlineServer(port, "org.ziggrid.driver.UpdateServlet");
		server.addStaticDir(staticContent);
		GPServletDefn servlet = server.getBaseServlet();
		servlet.setContextPath("/ziggrid");
		servlet.setServletPath("/updates");
		servlet.initParam("org.ziggrid.asyncSupportClass", "org.ziggrid.utils.http.ws.AsyncProcessor");
		servlet.addClassDir(staticContent);
	}

	@Override
	public void run() {
		server.run();
	}

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
}
