package org.ziggrid.generator.out.json;


public class LoadCouchbase { /* implements StorageEngine {
	public static Logger logger = LoggerFactory.getLogger("ZigGenerator");
	private CouchbaseClient conn;
	private List<OperationFuture<Boolean>> prev = new ArrayList<OperationFuture<Boolean>>();

	@Override
	public void open(IInterestEngine engine, IModel model, StorageConfig storage) {
		try {
//			URI server = new URI(f.couchUrl()+"pools");
//			List<URI> serverList = new ArrayList<URI>();
//			serverList.add(server);
//			CouchbaseConnectionFactoryBuilder builder = new CouchbaseConnectionFactoryBuilder();
//			builder.setOpTimeout(30000);
//			builder.setTimeoutExceptionThreshold(30000);
//			CouchbaseConnectionFactory ccf = builder.buildCouchbaseConnection(serverList, f.getBucket(), "");
//			conn = new CouchbaseClient(ccf);
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	@Override
	public short unique() {
		return 0;
	}

	@Override
	public StoreableObject findExisting(ListMap<String, ? extends ExistingObjectProvider> processors, String tlc, Map<String, Object> options) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean push(TickUpdate toSave) {
		List<OperationFuture<Boolean>> ops = new ArrayList<OperationFuture<Boolean>>();
		for (Entry<String, StoreableObject> x : toSave.updates.entrySet()) {
			try {
				ops.add(conn.set(x.getKey(), 0, x.getValue().asJsonString()));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		for (AnalyticItem ai : toSave.items) {
			try {
				ops.add(conn.set(ai.id(), 0, ai.asJsonString()));
			} catch (JSONException ex) {
				logger.error(ex.getMessage());
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
		return true;
	}

	@Override
	public void close() {
		conn.shutdown(1, TimeUnit.MINUTES);
	}

	@Override
	public void recordServer(String id, JSONObject obj) {
		conn.set(id, 0, obj.toString());
	}

	@Override
	public void syncTo(int id, int currentPosition) {
		throw new NotImplementedException();
	}

	@Override
	public boolean has(String gameId) {
		throw new NotImplementedException();
	}
*/
}
