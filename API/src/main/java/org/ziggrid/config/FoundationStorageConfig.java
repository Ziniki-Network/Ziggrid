package org.ziggrid.config;

import org.zinutils.collections.PeekableIterator;

public class FoundationStorageConfig extends StorageConfig {
	private final String cluster;
	private final boolean clearDB;
	private final boolean dodump;

	public FoundationStorageConfig(PeekableIterator<String> argi) {
		String uid = "--";
		String c = null;
		boolean cdb = false;
		boolean dod = false;
		while (true) {
			String peek = argi.peek();
			if (peek != null && peek.equals("--cluster")) {
				argi.accept();
				c = argi.next();
			} else if (peek != null && peek.equals("--clearDB")) {
				argi.accept();
				cdb = true;
			} else if (peek != null && peek.equals("--dump")) {
				argi.accept();
				dod = true;
			} else
				break;
		}
		cluster = c;
		clearDB = cdb;
		dodump = dod;
	}

	public String clusterFile() {
		return cluster;
	}

	public boolean clearDB() {
		return clearDB;
	}

	public boolean shouldDump() {
		return dodump;
	}

	@Override
	public String toString() {
		return "Foundation[]";
	}
}
