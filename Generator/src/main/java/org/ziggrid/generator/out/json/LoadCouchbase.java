package org.ziggrid.generator.out.json;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.internal.OperationFuture;

import org.codehaus.jettison.json.JSONException;
import org.ziggrid.generator.main.AnalyticItem;
import org.ziggrid.generator.main.HasCouchConn;
import org.ziggrid.generator.main.ZigGenerator;
import org.ziggrid.generator.out.AnalyticStore;
import org.ziggrid.generator.provider.Factory;
import org.ziggrid.utils.exceptions.UtilException;

import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.CouchbaseConnectionFactory;
import com.couchbase.client.CouchbaseConnectionFactoryBuilder;

public class LoadCouchbase implements AnalyticStore, HasCouchConn {
	private CouchbaseClient conn;
	private ZigGenerator gen;
	private List<OperationFuture<Boolean>> prev = new ArrayList<OperationFuture<Boolean>>();

	@Override
	public void open(Factory f) {
		try {
			URI server = new URI(f.couchUrl()+"pools");
			List<URI> serverList = new ArrayList<URI>();
			serverList.add(server);
			CouchbaseConnectionFactoryBuilder builder = new CouchbaseConnectionFactoryBuilder();
			builder.setOpTimeout(30000);
			builder.setTimeoutExceptionThreshold(30000);
			CouchbaseConnectionFactory ccf = builder.buildCouchbaseConnection(serverList, f.getBucket(), "");
			conn = new CouchbaseClient(ccf);
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	@Override
	public void push(List<AnalyticItem> toSave) {
		List<OperationFuture<Boolean>> ops = new ArrayList<OperationFuture<Boolean>>();
		for (AnalyticItem ai : toSave) {
			try {
				ops.add(conn.set(ai.id(), 0, ai.asJson()));
			} catch (JSONException ex) {
				ZigGenerator.logger.severe(ex.getMessage());
			}
		}
		try {
			for (OperationFuture<Boolean> op : prev) {
				if (!op.isDone())
					ops.add(op);
				else if (!op.get())
					throw new UtilException("An earlier operation failed");
			}
			prev = ops;
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	@Override
	public void close() {
		conn.shutdown(1, TimeUnit.MINUTES);
	}

	@Override
	public void setGenerator(ZigGenerator gen) {
		this.gen = gen;
	}

	@Override
	public CouchbaseClient getConnection() {
		return conn;
	}
}
