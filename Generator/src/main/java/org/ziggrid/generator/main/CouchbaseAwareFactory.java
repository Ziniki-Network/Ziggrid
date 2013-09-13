package org.ziggrid.generator.main;

import org.ziggrid.generator.provider.Factory;

import com.couchbase.client.CouchbaseClient;

public interface CouchbaseAwareFactory extends Factory {

	void setConnection(CouchbaseClient conn);

}
