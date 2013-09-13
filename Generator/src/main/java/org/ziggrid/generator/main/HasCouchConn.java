package org.ziggrid.generator.main;

import com.couchbase.client.CouchbaseClient;

public interface HasCouchConn {
	CouchbaseClient getConnection();
}
