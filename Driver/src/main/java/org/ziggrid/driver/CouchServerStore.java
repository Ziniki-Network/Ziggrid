package org.ziggrid.driver;

public class CouchServerStore { /* implements ServerStore {
	final static Logger logger = LoggerFactory.getLogger("CouchServerStore");
	private final String couchUrl;
	private final String bucket;
	private CouchbaseClient couch;
	private View nodesView;

	public CouchServerStore(String couchUrl, String bucket) {
		this.couchUrl = couchUrl;
		this.bucket = bucket;
		openCouch();
	}
	
	@Override
	public void register(String key, String value) {
		couch.set(key, 0, value);
	}

	@Override
	public void delete(ServerInfo s) {
		throw new NotImplementedException();
	}

	@Override
	public List<ServerInfo> list() {
		List<ServerInfo> ret = new ArrayList<ServerInfo>();
		Query q = new Query();
		q.setReduce(false);
		q.setStale(Stale.FALSE);
		for (ViewRow r : couch.query(nodesView, q)) {
			// TODO: I think these dates should come out of the row
			ret.add(new ServerInfo(r.getKey(), r.getValue(), new Date(), new Date()));
		}
		return ret;
	}

	public void openCouch() {
		try {
			logger.error("UpdateHandler opening private couchbase connection");
			URI server = new URI(couchUrl+"pools");
			List<URI> serverList = new ArrayList<URI>();
			serverList.add(server);
			CouchbaseConnectionFactoryBuilder builder = new CouchbaseConnectionFactoryBuilder();
			builder.setOpTimeout(30000);
			builder.setTimeoutExceptionThreshold(30000);
			CouchbaseConnectionFactory ccf = builder.buildCouchbaseConnection(serverList, bucket, "");
			couch = new CouchbaseClient(ccf);
			for (int i=0;i<4;i++) {
				try {
					nodesView = couch.getView("ziggrid", "nodes");
				} catch (InvalidViewException ex) {
					// if it's not there, put it there
					loadZiggridDesignDoc();
					SyncUtils.sleep(100);
				}
			}
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	@Override
	public void destroy() {
		logger.error("Update handler shutting down private couchbase connection");
		couch.shutdown(1, TimeUnit.MINUTES);
	}


	private void loadZiggridDesignDoc() {
		CouchDocument doc = new CouchDocument("ziggrid");
		CouchView view = doc.create("nodes");
		new JSCompiler(view.needMap().getBlock()) {
			public void compile() {
				ifNEq(var("meta").member("type"), string("json")).yes.returnVoid();
				ifFalsy(var("doc").member("webserver")).yes.returnVoid();
				voidFunction("emit", var("doc").member("webserver"), var("doc").member("endpoint"));
			}
		};
		try {
			HttpClient cli = new HttpClient();
			doc.writeToCouch(cli, couchUrl + "couchBase/" + bucket + "/");
		} catch (Exception ex) {
			logger.error("Failed to write " + doc.getName() + " to " + couchUrl, ex);
			SyncUtils.sleep(250);
		}
	}
*/}
