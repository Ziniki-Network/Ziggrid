package org.ziggrid.model.allocation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.model.Model;
import org.ziggrid.parsing.ErrorHandler;
import org.ziggrid.parsing.JsonReader;
import org.zinutils.collections.ListMap;
import org.zinutils.collections.SetMap;
import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.FileUtils;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;

public class AllocationTool {
	private static final Logger logger = LoggerFactory.getLogger("Ziggrid");
	private static boolean syntaxErrors;
	private static File privateKeyPath;
	private static Integer groupSize;
	private static Set<String> watchables = new TreeSet<String>();

	public static void main(String... args) {
		if (args.length != 4) {
			System.err.println("Usage: allocate dir bucket mappingFile s3://path");
			System.exit(1);
		}
			
		try {
			Model model = readModel(args[0], args[1]);
			if (model == null)
				System.exit(1);
			File mappingFile = new File(args[2]);
//			System.out.println("OK, " + (doInit?"writing to ":"reading from ") + mappingFile + " model = " + model);
			if (!partition(model, mappingFile, args[3]))
				System.exit(1);
		} catch (Exception ex) {
			ex.printStackTrace();
			System.err.println(ex.getMessage());
		}
	}
	
	private static boolean partition(Model model, File mappingFile, String s3path) throws IOException {
		String keypath = System.getProperty("org.ziggrid.s3key");
		if (keypath == null || keypath.length() == 0) {
			System.err.println("Must specify an S3 private key file in -Dorg.ziggrid.s3key");
			System.exit(1);
		}
		privateKeyPath = new File(keypath);
		if (!privateKeyPath.canRead()) {
			System.err.println("Cannot read S3 key file " + keypath);
			System.exit(1);
		}
		Properties props = new Properties();
		props.load(new FileInputStream(privateKeyPath));
	
		Pattern p = Pattern.compile("s3://([a-zA-Z0-9_]+)/(.+)/?");
		Matcher matcher = p.matcher(s3path);
		if (!matcher.matches())
			throw new UtilException("Could not match s3 path " + s3path);
		
		String s3bucket = matcher.group(1);
		String s3prefix = matcher.group(2);

		SetMap<ActivityKey, NodeUsage> allocations = parseMappingFile(model, mappingFile);
		if (syntaxErrors)
			return false;
		ListMap<String, String> options = invertAllocations(model, allocations);
		sendFilesToS3(options, s3bucket, s3prefix);
		return true;
	}

	private static SetMap<ActivityKey, NodeUsage> parseMappingFile(Model model, File mappingFile) throws FileNotFoundException, IOException {
		SetMap<ActivityKey, NodeUsage> allocations = new SetMap<ActivityKey, NodeUsage>();
		LineNumberReader lnr = new LineNumberReader(new FileReader(mappingFile));
		String s;
		ActivityKey activity = null;
		while ((s = lnr.readLine()) != null) {
//			System.out.println(lnr.getLineNumber() + ": " + s);
			String[] line = s.trim().split(" ");
			if (line == null || line.length == 0 || line[0].equals("") || line[0].equals("//"))
				continue;
			String cmd = line[0];
			if (cmd.equals("generator")) {
				if (line.length != 2) {
					error(lnr, s, "generator <config-file>");
					continue;
				}
				activity = ActivityKey.GENERATOR;
				activity.file = line[1];
			} else if (cmd.equals("observers")) {
				if (line.length != 1) {
					error(lnr, s, "observers");
					continue;
				}
				activity = ActivityKey.OBSERVER;
			} else if (cmd.equals("->")) {
				if (line.length < 3) {
					error(lnr, s, "-> node #threads");
					continue;
				}
				if (activity == null) {
					error(lnr, s, "cannot allocate nodes to an unspecified activity");
					continue;
				}
				String node = line[1];
				int nthreads = 1;
				String threadName = null;
				try {
					nthreads = Integer.parseInt(line[2]);
				} catch (NumberFormatException ex) {
					error(lnr, s, "number of threads must be an integer");
					continue;
				}
				if (nthreads < 1) {
					error(lnr, s, "number of threads must be at least one");
					continue;
				}
				allocations.add(activity, new NodeUsage(node, nthreads, threadName));
			} else if (cmd.equals("txgroup")) {
				activity = null;
				if (line.length != 2) {
					error(lnr, s, "txgroup <size>");
					continue;
				}
				if (groupSize != null)
				{
					error(lnr, s, "cannot specify group size more than once");
					continue;
				}
				try {
					groupSize = Integer.parseInt(line[1]);
					if (groupSize < 1)
					{
						error(lnr, s, "group size must be at least 1");
						continue;
					}
				} catch (NumberFormatException ex) {
					error(lnr, s, "group size must be a number");
					continue;
				}
			} else if (cmd.equals("watchable")) {
				activity = null;
				if (line.length < 2) {
					error(lnr, s, "watchable table");
					continue;
				}
				for (int i=1;i<line.length;i++)
					watchables.add(line[i]);
			} else {
				error(lnr, s, "cannot understand command '" + cmd + "'");
			}
		}
		lnr.close();
		return allocations;
	}

	// This is a mapping of node name to list of arguments
	private static ListMap<String, String> invertAllocations(Model model, SetMap<ActivityKey, NodeUsage> allocations) {
		ListMap<String, String> ret = new ListMap<String, String>();
		System.out.println(allocations);
		for (ActivityKey ak : allocations.keySet()) {
			int totThrs = 0;
			for (NodeUsage nu : allocations.get(ak)) {
				totThrs += nu.nthreads;
			}
			int usedThrs = 0;
			for (NodeUsage nu : allocations.get(ak)) {
				if (ak == ActivityKey.GENERATOR) {
					for (int i=0;i<nu.nthreads;i++) {
						ret.add(nu.node, "--generator");
						ret.add(nu.node, "file:"+ak.file);
						ret.add(nu.node, "--group");
						ret.add(nu.node, Integer.toString(usedThrs++));
						ret.add(nu.node, Integer.toString(totThrs));
					}
				} else {
					ret.add(nu.node, "--observer");
					ret.add(nu.node, "--threads");
					ret.add(nu.node, Integer.toString(usedThrs));
					ret.add(nu.node, Integer.toString(nu.nthreads));
					ret.add(nu.node, Integer.toString(totThrs));
					usedThrs += nu.nthreads;
				}
			}
		}
		for (String n : ret) {
			List<String> node = ret.get(n);
			if (groupSize != null) {
				node.add("--txgroup");
				node.add(Integer.toString(groupSize));
			}
			for (String s : watchables) {
				node.add("--watchable");
				node.add(s);
			}
		}
		return ret;
	}

	public static void sendFilesToS3(ListMap<String, String> options, String s3bucket, String s3prefix) throws IllegalArgumentException, IOException {
		AmazonS3 s3 = new AmazonS3Client(new PropertiesCredentials(privateKeyPath));
		TreeSet<String> nodes = new TreeSet<String>(options.keySet());
		for (String n : nodes) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			for (String o : options.get(n))
				pw.println(o);
			pw.flush();
			System.out.println(n);
			System.out.println(sw);
			byte[] bytes = sw.toString().getBytes();
			ByteArrayInputStream is = new ByteArrayInputStream(bytes);
			ObjectMetadata md = new ObjectMetadata();
			md.setContentType("text/plain");
			md.setContentLength(bytes.length);
			s3.putObject(s3bucket, s3prefix+"/"+n+".config", is, md);
		}
	}

	private static void error(LineNumberReader lnr, String s, String msg) {
		System.out.println(lnr.getLineNumber() +": " + msg);
		System.out.println("     " + s);
		syntaxErrors = true;
	}
	
	private static Model readModel(String dir, String bucket) {
		ErrorHandler eh = new ErrorHandler();
		try {
			File f = new File(dir, bucket);
			if (!f.isDirectory()) {
				eh.report(null, "The path " + f + " is not a directory");
				return null;
			}
			logger.info("Configuring bucket " + bucket);
			Model model = new Model();
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
			return eh.hasErrors()?null:model;
		} finally {
			eh.displayErrors();
		}
	}
}
