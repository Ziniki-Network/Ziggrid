package org.ziggrid.main;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.api.Definition;
import org.ziggrid.api.StorageEngine;
import org.ziggrid.config.ActingObserverConfig;
import org.ziggrid.config.CouchStorageConfig;
import org.ziggrid.config.CouchbaseObserverConfig;
import org.ziggrid.config.FoundationStorageConfig;
import org.ziggrid.config.MemoryStorageConfig;
import org.ziggrid.config.StorageConfig;
import org.ziggrid.driver.ActingCompositor;
import org.ziggrid.driver.ActingCorrelator;
import org.ziggrid.driver.ActingEnhancer;
import org.ziggrid.driver.ActingLeader;
import org.ziggrid.driver.ActingObserver;
import org.ziggrid.driver.ActingProcessor;
import org.ziggrid.driver.ActingSnapshot;
import org.ziggrid.driver.ActingSummarizer;
import org.ziggrid.driver.QueueMessageSource;
import org.ziggrid.driver.RawTapMessageSource;
import org.ziggrid.driver.ZiggridObserver;
import org.ziggrid.driver.interests.InterestEngine;
import org.ziggrid.driver.kv.FoundationStorage;
import org.ziggrid.driver.kv.MemoryStorage;
import org.ziggrid.generator.out.json.LoadCouchbase;
import org.ziggrid.kvstore.KVStorageEngine;
import org.ziggrid.main.WebConfig.Transpiler;
import org.ziggrid.model.CompositeDefinition;
import org.ziggrid.model.CorrelationDefinition;
import org.ziggrid.model.EnhancementDefinition;
import org.ziggrid.model.Grouping;
import org.ziggrid.model.LeaderboardDefinition;
import org.ziggrid.model.Model;
import org.ziggrid.model.ObjectDefinition;
import org.ziggrid.model.SnapshotDefinition;
import org.ziggrid.model.SummaryDefinition;
import org.ziggrid.parsing.ErrorHandler;
import org.ziggrid.parsing.JsonReader;
import org.ziggrid.parsing.ProcessingMethods;
import org.zinutils.collections.CollectionUtils;
import org.zinutils.collections.PeekableIterator;
import org.zinutils.exceptions.UtilException;
import org.zinutils.metrics.CodaHaleMetrics;
import org.zinutils.reflection.Reflection;
import org.zinutils.utils.DateUtils;
import org.zinutils.utils.FileUtils;
import org.zinutils.utils.ZUJarEntry;
import org.zinutils.utils.ZUJarFile;
import org.zinutils.xml.XML;

public class Ziggrid {
	private static final Logger logger = LoggerFactory.getLogger("Ziggrid");
	private ErrorHandler eh = new ErrorHandler();
	List<Thread> threads = new ArrayList<Thread>();
	WebServer webServer = null;
	private static final int metricSamplingFreq = 10;
	ProcessingMethods pm = new ProcessingMethods();
	int useView = 0;
	private Model model;
	private StorageConfig storage;
	Class<?> storageEngineClass = null;
	private Map<String, ActingProcessor> processors = new TreeMap<String, ActingProcessor>();// TODO: I don't think this will work with Couch
	private List<Object> configs = new ArrayList<Object>();
	private TreeSet<String> readFiles = new TreeSet<String>();
	private List<GeneratorDefn> generatorDefns = new ArrayList<GeneratorDefn>();
	private int txGroupSize = 1;

	public static void main(String[] args) {
        Ziggrid me = new Ziggrid();
        PeekableIterator<String> argi = CollectionUtils.peekableIterator(args);
        try {
        	me.processArguments(argi);
        } catch (Exception ex) {
        	ex.printStackTrace();
        	me.eh.report(null, ex.getMessage());
        }
		if (argi.hasNext())
			me.eh.report(null, "There were unprocessed arguments");
        if (me.hasErrors()) {
        	me.showErrors();
        	usage();
        	System.exit(1);
        }
        me.execute();
	}

	Ziggrid() {
	}
	
	public void processArguments(PeekableIterator<String> argi) {
        while (argi.hasNext()) {
        	String opt = argi.next();
        	if (opt.equals("--model")) {
        		readModel(argi);
        	} else if (opt.equals("--storage")) {
        		readStorage(argi);
        	} else if (opt.equals("--file")) {
        		String path = argi.next();
        		if (!readFiles.contains(path)) {
        			readFiles.add(path);
        			File file = new File(path);
        			if (file.canRead()) {
        				List<String> lines = new ArrayList<String>();
        				for (String s : FileUtils.readFileAsLines(file))
        					lines.add(s.trim());
        				processArguments(new PeekableIterator<String>(lines));
        			} else
        				eh.report(null, "Cannot read file " + path);
        		}
        	} else if (model == null || storage == null) {
        		eh.report(null, "You must specify model and storage before other options");
        		return;
        	} else if (opt.equals("--watchable")) {
        		handleWatching(argi);
        	} else if (opt.equals("--generator")) {
        		readGenerator(argi);
        	} else if (opt.equals("--observer")) {
        		readObserver(argi);
        	} else if (opt.equals("--txgroup")) {
        		txGroupSize = Integer.parseInt(argi.next());
        	} else if (opt.equals("--web")) {
        		readWeb(argi);
        	} else if (opt.equals("--useView")) {
        		try {
        			pm.useView(argi.next());
        			useView++;
        		} catch (Exception ex) {
        			eh.report(null, ex.getMessage());
        			return;
        		}
        	} else if (opt.equals("--metricsDir")) {
				String metricsDir = argi.next();

				File dir = new File(metricsDir);
				if(!dir.isDirectory()) {
					System.err.println("The directory \"" + metricsDir + "\" is not a directory or does not exist.");
					System.exit(1);
				}
				CodaHaleMetrics.configureReports(metricsDir, metricSamplingFreq);
        	} else if (opt.equals("--graphite")) {
        		String name = argi.next();
				String graphiteHost = argi.next();
				int graphitePort = Integer.parseInt(argi.next());
				CodaHaleMetrics.configureGraphite(name, graphiteHost, graphitePort, metricSamplingFreq);
        	} else
        		eh.report(null, "Unrecognized argument: " + opt);
        }
        
        if (model == null || storage == null) {
			eh.report(null, "You must specify model and storage");
			return;
        }

		if (storageEngineClass == null) {
 			if (storage instanceof MemoryStorageConfig)
 				storageEngineClass = MemoryStorage.class;
 			else if (storage instanceof FoundationStorageConfig)
 				storageEngineClass = FoundationStorage.class;
 			else if (storage instanceof CouchStorageConfig)
 				storageEngineClass = LoadCouchbase.class;
 			else {
    			eh.report(null, "Cannot figure the storage engine");
 				return;
 			}
        }

		/*
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
		*/
		
		/*
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
					ArrayList<KVQueue> queues = new ArrayList<KVQueue>();
					for (ConnectorSpec cs : connectors) {
						Object conn = Reflection.create(Ziggrid2.class.getClassLoader(), cs.clz, dbUrl, bucket, cs.args);
						if (conn instanceof MessageSource)
							sources.add((MessageSource) conn);
						else if (conn instanceof KVQueue)
							queues.add((KVQueue)conn);
						else if (conn instanceof Runnable)
							runnable.add((Runnable)conn);
						else
							throw new UtilException("Cannot process " + cs.clz + " as a connector");
					}
					if (sources.isEmpty() && queues.isEmpty()) {
						System.err.println("There are no valid queuing sources for bucket " + bucket);
						continue;
					}
						 
					ZiggridObserver observer;
					if (memoryMode)
						observer = new InMemoryObserver(model, queues);
					else if (foundationMode)
						throw new UtilException("Foundation mode is not yet supported");
					else { // couchbase mode
						observer = new CouchbaseObserver(dbUrl, bucket, pm, model, sources, max, gating);
						((CouchbaseObserver) observer).createViews();
					}
					
					observers.add(observer);
					*/
	}

	private void readModel(Iterator<String> argi) {
		String dir = argi.next();
		String bucket = argi.next();
		List<ResourceFile> allFiles = getMeTheFiles(dir, bucket);
		if (allFiles == null)
			return;
		logger.info("Configuring bucket " + bucket);
		model = new Model();
		for (ResourceFile mf : allFiles) {
			logger.info("Reading " + mf);
			JsonReader jr = new JsonReader();
			jr.readModel(eh, model, mf.getDocument(), mf.getContents());
		}
		model.validate(eh);
	}

	private List<ResourceFile> getMeTheFiles(String dir, String bucket) {
		if (dir.startsWith("resource:"))
			return getResourceFiles(dir.replace("resource:",""), bucket);
		else if (dir.startsWith("file:"))
			dir = dir.replace("file:","");
		else if (dir.startsWith("dir:"))
			dir = dir.replace("dir:","");
		File f = new File(dir, bucket);
		if (!f.isDirectory()) {
			eh.report(null, "The path " + f + " is not a directory");
			return null;
		}
		ArrayList<ResourceFile> ret = new ArrayList<ResourceFile>();
		include(ret, f);
		return ret;
	}

	public void include(ArrayList<ResourceFile> ret, File f) {
		for (File g : f.listFiles()) {
			if (g.isDirectory()) {
				logger.info("Found document directory " + g.getName());
				for (File h : g.listFiles())
					if (h.getName().endsWith(".json"))
						ret.add(new ResourceFile(g, h));
			}
		}
	}

	private List<ResourceFile> getResourceFiles(String doc, String bucket) {
		ArrayList<ResourceFile> ret = new ArrayList<ResourceFile>();
		List<File> pathElts = FileUtils.splitJavaPath(System.getProperty("java.class.path"));
		for (File f : pathElts) {
			if (f.isDirectory()) {
				File from = FileUtils.combine(f, doc, bucket);
				if (from.isDirectory()) {
					include(ret, from);
				}
			} else if (f.isFile() && f.getName().endsWith(".jar")) {
				ZUJarFile jf = new ZUJarFile(f);
				String lookFor = doc + "/" + bucket + "/";
				System.out.println("looking for " + lookFor);
				for (ZUJarEntry je : jf) {
					String name = je.getName();
					if (!name.startsWith(lookFor) || !name.endsWith(".json"))
						continue;
					name = name.replace(lookFor, "");
					File total = new File(name);
					String docName = total.getParent();
					System.out.println("Looking at " + name + " " + docName);
					ret.add(new ResourceFile(docName, name, je.getBytes()));
				}
			}
		}
		return ret;
	}

	private void readStorage(PeekableIterator<String> argi) {
		String opt = argi.next();
		if (opt.equals("couch")) {
			storage = new CouchStorageConfig(argi);
		} else if (opt.equals("memory")) {
			storage = new MemoryStorageConfig(argi);
		} else if (opt.equals("foundation")) {
			storage = new FoundationStorageConfig(argi);
		} else
			eh.report(null, "There is no storage mechanism called '" + opt + "'");
	}

	private void handleWatching(PeekableIterator<String> argi) {
		String input = argi.next();
		model.selectProcessorsFor(input);
	}

	private void readGenerator(PeekableIterator<String> argi) {
		if (!argi.hasNext()) {
			eh.report(null,  "--generator file [--limit n] [--group n m]");
			return;
		}
		String input = argi.next();
		XML generatorXML = null;
		int limit = -1;
		int genMod = -1;
		int genOutOf = -1;
		try {
			generatorXML = XML.fromContainer(input);
		} catch (Exception ex) {
			eh.report(null, "There is no XML container: " + input);
		}
		while (true) {
			if (argi.hasNext() && argi.peek().equals("--limit")) {
				argi.next(); // --limit
				limit = Integer.parseInt(argi.next());
			} else if (argi.hasNext() && argi.peek().equals("--group")) {
				argi.next(); // --group
				genMod = Integer.parseInt(argi.next());
				genOutOf = Integer.parseInt(argi.next());
			} else
				break;
		}
		generatorDefns.add(new GeneratorDefn(generatorXML, limit, genMod, genOutOf));
	}
	
	private void readObserver(PeekableIterator<String> argi) {
		Object obs = null;
		if (storage instanceof MemoryStorageConfig || storage instanceof FoundationStorageConfig) {
			obs = new ActingObserverConfig(argi);
		} else if (storage instanceof CouchStorageConfig) {
			String source = argi.next();
			if (source.equals("tap"))
				obs = new CouchbaseObserverConfig(RawTapMessageSource.class);
			else if (source.equals("amqp"))
				obs = new CouchbaseObserverConfig(QueueMessageSource.class);
			else
				eh.report(null, "There is no source called " + source);
		} else
			throw new UtilException("Cannot handle storage mechanism " + storage + " when processing observer");
		configs.add(obs);
	}

	private void readWeb(PeekableIterator<String> argi) {
		WebConfig web = null;
		if (storage instanceof MemoryStorageConfig || storage instanceof FoundationStorageConfig) {
			web = new WebConfig(null, storage, model);
		} else if (storage instanceof CouchStorageConfig) {
			String source = argi.next();
			if (source.equals("tap"))
				web = new WebConfig(RawTapMessageSource.class, storage, model);
			else if (source.equals("amqp"))
				web = new WebConfig(RawTapMessageSource.class, storage, model);
			else
				eh.report(null, "There is no source called " + source);
		} else
			throw new UtilException("Cannot handle storage mechanism " + storage + " when configuring web server");
		while (argi.hasNext()) {
			if (argi.peek().equals("--port")) {
				argi.next();
				web.setPort(Integer.parseInt(argi.next()));
			} else if (argi.peek().equals("--resource")) {
				argi.next();
				web.addResourceStaticDir(argi.next());
			} else if (argi.peek().equals("--static")) {
				argi.next();
				web.addStaticDir(argi.next());
			} else if (argi.peek().equals("--transpiler")) {
				argi.next();
				web.transpilerModuleDir(argi.next());
			} else if (argi.peek().equals("--js") || argi.peek().equals("--ember")) {
				// --js /assets/app.js /Users/gareth/Ziniki/Code/beane-counter-ui/app ../tmp/transpiled/app --prefix appkit
				Transpiler tr = web.newTranspiler(argi.next());
				tr.path(argi.next());
				File from = new File(argi.next());
				tr.from(from);
				tr.tmp(FileUtils.combine(from, argi.next()));
				if (argi.peek().equals("--prefix")) {
					argi.next();
					tr.prefix(argi.next());
				}
			} else
				break;
		}
		configs.add(web);
	}

	private void execute() {
		StorageEngine store = null;
		ArrayList<ZiggridObserver> observers = new ArrayList<ZiggridObserver>();
		logger.info("Executing ...");
		Date from = new Date();
		try {
			store = (StorageEngine) Reflection.create(storageEngineClass);
			InterestEngine interests = new InterestEngine(store, model);
			store.open(interests, model, storage);
			if (store instanceof MemoryStorage || store instanceof FoundationStorage) {
				createProcessors((KVStorageEngine)store, interests);
			}
			for (Object o : configs) {
				if (o instanceof WebConfig) {
					WebConfig c = (WebConfig)o;
					if (webServer != null) {
						System.err.println("It's not possible to have more than one web server per process because of the main-thread/event-loop restriction");
						System.exit(1);
					}
					webServer = new WebServer(c);
				} else {
					if (store instanceof MemoryStorage || store instanceof FoundationStorage) {
						addActingObserver(observers, ((ActingObserverConfig)o), (KVStorageEngine) store, interests);
					} else if (storage instanceof CouchStorageConfig) {
//						addObserver(observers, new CouchbaseObserver((CouchStorageConfig)storage));
					} else {
						throw new UtilException("Something got lost somewhere");
					}
				}
			}
			logger.info("Created " + observers.size() + " observers");
			if (webServer != null)
				webServer.addConnectionServlet(interests);
			if (!observers.isEmpty() && webServer != null)
				webServer.addObserverServlet(interests);
			if (!generatorDefns.isEmpty()) {
				List<ZigGenerator> generators = new ArrayList<ZigGenerator>();
				for (GeneratorDefn d : generatorDefns) {
					ZigGenerator g = new ZigGenerator(false, 0, d.limit, d.generatorXML, store, model);
					if (d.genOutOf != -1) {
						g.factory.setPosition(store.unique(), d.genOutOf, d.genMod);
					}
					generators.add(g);
				}
				if (webServer != null)
					webServer.addGeneratorServlet(generators);
				else {
					for (ZigGenerator g : generators)
						threads.add(new GeneratorThread(null, g));
				}
			}
			for (Thread t : threads)
				t.start();
			if (webServer != null)
				webServer.run();
		} finally {
			for (Thread t : threads)
				try {
					t.join();
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			if (store != null)
				store.close();
			logger.info("Run completed in " + DateUtils.elapsedTime(from, new Date(), DateUtils.Format.hhmmss3));
		}
	}

	private void addActingObserver(ArrayList<ZiggridObserver> observers, ActingObserverConfig config, KVStorageEngine store, InterestEngine interests) {
		int max = config.getThreadCount();
		int[] split = new int[max+1];
		for (int i=0;i<max;i++) {
			if (max > 256)
				split[i] = i*65536/max;
			else
				split[i] = i*256/max;
		}
		split[max] = (max > 256)?65536:256;
		for (int k = config.getFromThread();k<config.getFromThread() + config.getNumThreads();k++) {
			ActingObserver zo = new ActingObserver(model, store, processors, config.stayAliveFor(), txGroupSize, interests, split[k], split[k+1], split[max]);
			addObserver(observers, zo);
		}
	}
	
	private void createProcessors(KVStorageEngine store, InterestEngine interests) {
		for (Definition d : model.definitions.values()) {
			String sha = model.shaFor(d);
			if (d instanceof ObjectDefinition)
				; // not observable here
			else if (d instanceof EnhancementDefinition) {
				EnhancementDefinition enh = (EnhancementDefinition)d;
				addProcessor(interests, sha, new ActingEnhancer(model.shaFor(d), store, enh));
			} else if (d instanceof SummaryDefinition) {
				SummaryDefinition sd = (SummaryDefinition)d;
				for (int k=0;k<=sd.matches.size();k++) {
					sha = model.shaFor(sd, "key"+k);
					addProcessor(interests, sha, new ActingSummarizer(model, store, sd, k));
				}
			} else if (d instanceof LeaderboardDefinition) {
				LeaderboardDefinition ld = (LeaderboardDefinition)d;
				for (Grouping grp : ld.groupings()) {
					sha = model.shaFor(d, grp.asGroupName());
					addProcessor(interests, sha, new ActingLeader(sha, store, interests, ld, grp));
				}
			} else if (d instanceof SnapshotDefinition) {
				SnapshotDefinition sd = (SnapshotDefinition) d;
				sha = model.shaFor(sd);
				addProcessor(interests, sha, new ActingSnapshot(sha, store, sd));
			} else if (d instanceof CompositeDefinition) {
				CompositeDefinition cd = (CompositeDefinition) d;
				addProcessor(interests, sha, new ActingCompositor(sha, store, cd));
			} else if (d instanceof CorrelationDefinition) {
				CorrelationDefinition cd = (CorrelationDefinition) d;
				for (Grouping grp : cd.groupings()) {
					sha = model.shaFor(cd, grp.asGroupName());
					addProcessor(interests, sha, new ActingCorrelator(sha, store, cd, grp));
				}
			}
			else
				throw new UtilException("Need an observer for " + d.getClass() + " => " + d);
		}
		
		for (Entry<String, ActingProcessor> kv : processors.entrySet()) {
			logger.info("Processor " + kv.getKey() + " => " + kv.getValue());
		}
	}

	public void addProcessor(InterestEngine interests, String sha, ActingProcessor proc) {
		if (hasWatchable(proc)) {
			processors.put(sha, proc);
			interests.processorExists(proc);
		}
	}

	private boolean hasWatchable(ActingProcessor proc) {
		for (String s : proc.watchables())
			if (model.restrictionIncludes(s))
				return true;
		return false;
	}

	private void addObserver(ArrayList<ZiggridObserver> observers, ZiggridObserver zo) {
		Thread obsThread = new Thread(zo, "Observer-"+observers.size());
		observers.add(zo);
		threads.add(obsThread);
		logger.info("  Running observer " + zo + " in thread " + obsThread.getName());
	}

	private boolean hasErrors() {
		return eh.hasErrors();
	}
	
	private void showErrors() {
		eh.displayErrors();
	}

	public static void usage() {
		System.err.println("Usage: ziggrid [options]");
		System.err.println("  where the options are:");
		System.err.println("    --model <root-directory> <choice>");
		System.err.println("    --storage [memory|couch|foundation] options");
		System.err.println("    --generator options");
		System.err.println("    --observer options");
		System.err.println("    --web options");
		System.err.println("  You must specify the model and storage before the other options");
		System.err.println("  There are also other specific options you can use, including");
		System.err.println("    --metricsDir dir    - a directory to store metrics");
		System.err.println("    --useView processor - use view for <processor>");
		System.exit(1);
	}

	public class ResourceFile {
		private final String document;
		private final File fromFile;
		private final byte[] bytes;
		private final String callMe;

		public ResourceFile(File document, File actual) {
			this.document = document.getName();
			this.fromFile = actual;
			this.callMe = actual.getPath();
			this.bytes = null;
		}

		public ResourceFile(String docName, String callMe, byte[] bytes) {
			document = docName;
			this.callMe = callMe;
			this.fromFile = null;
			this.bytes = bytes;
		}

		public String getDocument() {
			return document;
		}

		public String getContents() {
			if (fromFile != null)
				return FileUtils.readFile(fromFile);
			else
				return new String(bytes);
		}
		
		@Override
		public String toString() {
			return callMe;
		}
	}
}
