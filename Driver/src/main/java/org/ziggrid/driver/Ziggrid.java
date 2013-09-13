package org.ziggrid.driver;

import java.io.File;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.model.CompositeDefinition;
import org.ziggrid.model.CorrelationDefinition;
import org.ziggrid.model.Definition;
import org.ziggrid.model.EnhancementDefinition;
import org.ziggrid.model.IndexDefinition;
import org.ziggrid.model.Model;
import org.ziggrid.model.ObjectDefinition;
import org.ziggrid.model.SnapshotDefinition;
import org.ziggrid.model.SummaryDefinition;
import org.ziggrid.parsing.ErrorHandler;
import org.ziggrid.parsing.JsonReader;
import org.ziggrid.utils.collections.CollectionUtils;
import org.ziggrid.utils.collections.SetMap;
import org.ziggrid.utils.metrics.CodeHaleMetrics;
import org.ziggrid.utils.reflection.Reflection;
import org.ziggrid.utils.utils.FileUtils;
import org.ziggrid.utils.utils.PrettyPrinter;


public class Ziggrid {
	private static final int metricSamplingFreq = 10;
	public static final Logger logger = LoggerFactory.getLogger("Ziggrid");
	static final int groupSize = 50;
	// This is needed in UpdateHandler.  Since all the ways of doing this I can see appear to be some kind of backdoor, I just reference it directly
	static List<Observer> observers = new ArrayList<Observer>();

	public static void main(String[] args) {
        if (args.length < 3) {
			System.err.println("Usage: ziggrid couchUrl config [--connector class options ...]");
			System.err.println("  known connectors include:");
			System.err.println("    org.ziggrid.driver.WebServer - for communicating with browsers");
			System.err.println("    org.ziggrid.driver.TapServer - for reading Tap information from Couchbase");
			System.err.println("    org.ziggrid.driver.MQServer  - for reading distributed Tap information from an intermediate MQ server");
			System.err.println("  see each class javadoc for available options");
			System.err.println("  at least one connector providing Tap information must be provided");
			System.exit(1);
		}
		String couchUrl = args[0];
		File config = new File(args[1]);
		if (!config.isDirectory()) {
			System.err.println("Config must be a directory");
			System.exit(1);
		}

		int connectorArgumentsFrom = 2;
		
		if (args[2].equals("--metricsDir"))
		{
			if (args.length < 4)
			{
				System.err.println("--metricsDir needs a directory argument");
				System.exit(1);
			}
			
			String metricsDir = args[3];
			
			File dir = new File(metricsDir);
			if(!dir.isDirectory()) {
				System.err.println("The directory \"" + metricsDir + "\" is not a directory or does not exist.");
				System.exit(1);
			}
			connectorArgumentsFrom+=2;
			CodeHaleMetrics.configureReports(metricsDir, metricSamplingFreq);
		}
		
		List<ConnectorSpec> connectors = new ArrayList<ConnectorSpec>();
		while (connectorArgumentsFrom < args.length) {
			if (!args[connectorArgumentsFrom].equals("--connector"))
			{
				System.err.println("Did not see --connector");
				System.exit(1);
			}
			if (connectorArgumentsFrom + 1 >= args.length) {
				System.err.println("--connector needs a class argument");
				System.exit(1);
			}
			int opts = connectorArgumentsFrom + 2;
			while (opts < args.length && !args[opts].equals("--connector"))
				opts++;
			Object[] connArgs = new Object[opts-connectorArgumentsFrom-2];
			for (int i=connectorArgumentsFrom+2;i<opts;i++)
				connArgs[i-connectorArgumentsFrom-2] = args[i];
			connectors.add(new ConnectorSpec(args[connectorArgumentsFrom+1], connArgs));
			connectorArgumentsFrom = opts;
		}
		
		List<Thread> threads = new ArrayList<Thread>();
		List<Runnable> runnable = new ArrayList<Runnable>();
		try {
			for (File f : config.listFiles())
				if (f.isDirectory()) {
					String bucket = f.getName();
					logger.info("Configuring bucket " + bucket);
					Model model = new Model();
					ErrorHandler eh = new ErrorHandler();
					for (File df : f.listFiles()) {
						logger.info("Found document directory " + df.getName());
						for (File mf : df.listFiles()) {
							if (mf.getName().endsWith(".json")) {
								logger.info("Reading " + mf.getPath());
								JsonReader jr = new JsonReader();
								jr.readModel(eh, model, df.getName(), FileUtils.readFile(mf));
							}
						}
					}
					model.validate(eh);
					StringWriter sw = new StringWriter();
					model.prettyPrint(new PrintWriter(sw));
					LineNumberReader lnr = new LineNumberReader(new StringReader(sw.toString()));
					try {
						String s1;
						while ((s1 = lnr.readLine()) != null)
							logger.info(s1);
					} catch (Exception ex) {
						logger.error("Exception printing model", ex);
					}
					if (eh.displayErrors())
						return;
					
					SetMap<String, Integer> depths = new SetMap<String, Integer>();
					for (Definition x : model.dag.roots()) {
						if (x instanceof ObjectDefinition) {
							PrettyPrinter pp = new PrettyPrinter();
							followDown(model, depths, pp, x);
						} else if (x instanceof IndexDefinition) {
							IndexDefinition id = (IndexDefinition) x;
							ObjectDefinition idm = model.getModel(eh, id.ofType);
							PrettyPrinter pp = new PrettyPrinter();
							followDown(model, depths, pp, idm);
						}
					}
					for (Definition x : model.dag.nodes())
						if (x instanceof ObjectDefinition && !depths.contains(((ObjectDefinition)x).name))
							eh.report(x, "not added to DAG");
					int max = 0;
					TreeMap<String, Integer> gating = new TreeMap<String, Integer>();
					for (String s : depths.keySet())
						if (depths.size(s) != 1)
							eh.report(null, "Cannot handle different depths for object " + s + ": " + depths.get(s));
						else  {
							Integer v = CollectionUtils.any(depths.get(s));
							max = Math.max(max, v);
							gating.put(s, v);
						}
					if (eh.displayErrors())
						return;
					logger.info("Gating depth = " + max);
					logger.info("Object depths = " + gating);
					
					ArrayList<MessageSource> sources = new ArrayList<MessageSource>();
					for (ConnectorSpec cs : connectors) {
						Object conn = Reflection.create(Ziggrid.class.getClassLoader(), cs.clz, couchUrl, bucket, cs.args);
						if (conn instanceof MessageSource)
							sources.add((MessageSource) conn);
						else if (conn instanceof Runnable)
							runnable.add((Runnable)conn);
					}
					if (sources.isEmpty()) {
						System.err.println("There are no valid TAP sources for bucket " + bucket);
						continue;
					}
						 
					Observer observer = new Observer(couchUrl, bucket, model, sources, max, gating);
					
					observers.add(observer);
					observer.createViews();
					Thread obsThread = new Thread(observer, "Observer");
					obsThread.start();
					threads.add(obsThread);
				}
			if (runnable.size() == 1) {
				runnable.get(0).run();
			} else if (runnable.size() > 1) {
				System.err.println("Currently it's not possible to have more than one Runnable because of the event-loop issue");
				System.exit(1);
			}
		} finally {
			for (Thread t : threads)
				try {
					t.join();
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			for (Observer o : observers)
				o.close();
		}
	}

	private static int followDown(Model model, SetMap<String, Integer> depths, PrettyPrinter pp, Definition x) {
		pp.indentMore();
		int depth = 0;
		for (Definition y : model.dag.children(x))
			depth = Math.max(depth, followDown(model, depths, pp, y));
		if (x instanceof ObjectDefinition)
			depths.add(((ObjectDefinition)x).name, depth);
		else if (x instanceof EnhancementDefinition || x instanceof SummaryDefinition || x instanceof CompositeDefinition || x instanceof CorrelationDefinition || x instanceof SnapshotDefinition)
			depth += 1;
		else
			depth += 10;
		pp.indentLess();
		pp.append(x + ":" + depth);
		return depth;
	}
}

class ConnectorSpec {
	String clz;
	Object[] args;

	public ConnectorSpec(String clz, Object[] args) {
		this.clz = clz;
		this.args = args;
	}
}

