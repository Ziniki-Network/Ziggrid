package org.ziggrid.generator.main;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.ziggrid.generator.out.AnalyticStore;
import org.ziggrid.generator.provider.Factory;
import org.ziggrid.utils.http.GPServletDefn;
import org.ziggrid.utils.http.InlineServer;
import org.ziggrid.utils.http.NotifyOnServerReady;
import org.ziggrid.utils.reflection.Reflection;
import org.ziggrid.utils.serialization.Endpoint;
import org.ziggrid.utils.sync.SyncUtils;
import org.ziggrid.utils.utils.DateUtils;
import org.ziggrid.utils.xml.XML;

import com.couchbase.client.CouchbaseClient;

public class ZigGenerator {
	private InlineServer server;
	private final Factory factory;
	private final AnalyticStore analyticStore;
	private Timer timer;
	public static Logger logger = Logger.getLogger("ZigGenerator");
	private int endTime;
	public int delay;
	public final int limit;
	private int count = 0;
	static ZigGenerator instance;

	public static void main(String[] argv) throws IOException, URISyntaxException {
		AnalyticStore store = null;
		String config = null;
		boolean usage = false;
		boolean realtime = false;
		int delay = 0;
		int limit = 0;
		int port = 0;
		for (int i=0;i<argv.length && !usage;i++) {
			if ("--realtime".equals(argv[i]))
				realtime = true;
			else if ("--delay".equals(argv[i]))
				delay = Integer.parseInt(argv[++i]);
			else if ("--limit".equals(argv[i]))
				limit = Integer.parseInt(argv[++i]);
			else if ("--server".equals(argv[i]))
				port = Integer.parseInt(argv[++i]);
			else if (argv[i].startsWith("--"))
				usage = true;
			else if (store == null)
				store = Reflection.create(ZigGenerator.class.getClassLoader(), argv[i]);
			else if (config == null)
				config = argv[i];
			else
				usage = true;
		}
		if (usage || config == null)
		{
			System.out.println("Usage: Generator [--realtime] [--limit #] [--delay #] [--server port] store config");
			System.exit(1);
		}
		final ZigGenerator generator = new ZigGenerator(realtime, delay, limit, config, store);
		if (port != 0) {
			instance.configureServer(port);
			if (store != null && store instanceof HasCouchConn) {
				final AnalyticStore fstore = store;
				instance.server.notify(new NotifyOnServerReady() {
					public void serverReady(InlineServer inlineServer, Endpoint addr) {
						String endpoint = addr.toString();
						CouchbaseClient conn = ((HasCouchConn)fstore).getConnection();
						conn.set("webServer/" + endpoint, 0, "{\"webserver\":\"generator\",\"endpoint\":\"" + endpoint + "\"}");
						if (generator.factory instanceof CouchbaseAwareFactory)
							((CouchbaseAwareFactory)generator.factory).setConnection(conn);
					}
				});
			}
			Date from = new Date();
			instance.init();
			instance.server.run();
			instance.close(from);
		}
		else 
			instance.doAll();
	}

	public ZigGenerator(boolean realtime, int delay, int limit, String configFile, AnalyticStore store) {
		this.delay = delay;
		this.limit = limit;
		XML config = XML.fromContainer(configFile);
		String factoryClass = config.top().get("factory");
		factory = Reflection.create(getClass().getClassLoader(), factoryClass, config);
		timer = new Timer(realtime, 0);
		endTime = factory.endAt();
		this.analyticStore = store;
		store.setGenerator(this);
		instance = this;
	}

	private void configureServer(int port) {
		server = new InlineServer(port, "org.ziggrid.generator.main.ControlServlet");
		GPServletDefn servlet = server.getBaseServlet();
		servlet.setContextPath("/ziggrid");
		servlet.setServletPath("/generator");
		servlet.initParam("org.ziggrid.asyncSupportClass", "org.ziggrid.utils.http.ws.AsyncProcessor");
		servlet.addClassDir(new File("."));
	}

	public ZigGenerator setEndTime(int endTime) {
		this.endTime = endTime;
		return this;
	}
	
	public void doAll() {
		Date from = new Date();
		try {
			init();
			while (!timeUp())
				if (!advanceOneTick())
					break;
		} catch (Throwable t) {
			System.err.println("Error encountered: " + t.getMessage());
		} finally {
			close(from);
		}
	}
	
	public void resetCounter() {
		count = 0;
	}

	public boolean timeUp() {
		return !(endTime == -1 || timer.lessThan(endTime));
	}

	public void init() {
		factory.prepareRun();
		analyticStore.open(factory);
	}

	public boolean advanceOneTick() {
		logger.info("Generating events at " + timer + "; end = " + endTime);
		List<AnalyticItem> toSave = factory.doTick(timer);
		if (toSave == null)
			return false;
		analyticStore.push(toSave);
		count += toSave.size();
		if (limit > 0 && count > limit)
			return false;
		timer.next();
		if (delay > 0)
			SyncUtils.sleep(delay);

		return true;
	}
	
	public void close(Date from) {
		logger.info("Closing down ...");
		analyticStore.close();
		factory.close();
		logger.info("Run completed in " + DateUtils.elapsedTime(from, new Date(), DateUtils.Format.hhmmss3));
	}

	public String currentTick() {
		return timer.toString();
	}
}
