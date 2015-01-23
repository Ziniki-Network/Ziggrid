package org.ziggrid.main;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.ziggrid.config.StorageConfig;
import org.ziggrid.model.Model;

/* I think it's theoretically possible to extend this class to support
 * more options, but we don't currently need to do that
 */
public class WebConfig {
	public class Transpiler {
		public String path;
		public String style;
		private String prefix;
		public File from;
		public File tmp;

		public Transpiler(String style) {
			this.style = style;
		}

		public void path(String path) {
			this.path = path;
		}

		public void from(File from) {
			this.from = from;
			
		}

		public void tmp(File tmp) {
			this.tmp = tmp;
		}

		public void prefix(String p) {
			this.prefix = p;
		}

		public String prefix() {
			if (prefix == null)
				return "";
			return prefix + "/";
		}
	}

	private final Class<?> source;
	private final StorageConfig storage;
	private final Model model;
	private final List<File> staticDirs = new ArrayList<File>();
	private final List<String> staticResources = new ArrayList<String>();
	private int port = 10091;
	public String transpilerModuleDir;
	final List<Transpiler> transpilers = new ArrayList<Transpiler>();

	public WebConfig(Class<?> source, StorageConfig storage, Model model) {
		this.source = source;
		this.storage = storage;
		this.model = model;
	}
	
	public void setPort(int port) {
		this.port = port;
	}

	public int getPort() {
		return port;
	}

	public StorageConfig getStorage() {
		return storage;
	}

	public void addStaticDir(String dir) {
		staticDirs.add(new File(dir));
	}

	public void addResourceStaticDir(String prefix) {
		if (!prefix.startsWith("/"))
			prefix = "/" + prefix;
		while (prefix.endsWith("/"))
			prefix = prefix.substring(0, prefix.length()-1);
		staticResources.add(prefix);
	}
	
	public List<File> getStaticDirs() {
		return staticDirs;
	}

	public void transpilerModuleDir(String dir) {
		this.transpilerModuleDir = dir;
	}
	
	public Transpiler newTranspiler(String style) {
		Transpiler tr = new Transpiler(style);
		transpilers.add(tr);
		return tr;
	}

	public List<String> getStaticResources() {
		return staticResources;
	}
}
