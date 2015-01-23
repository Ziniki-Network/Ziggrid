package org.ziggrid.config;

import java.util.Iterator;

public class CouchStorageConfig extends StorageConfig {
	public final String dbUrl;
	public String bucket;

	public CouchStorageConfig(Iterator<String> argi) {
		dbUrl = argi.next();
	}

}
