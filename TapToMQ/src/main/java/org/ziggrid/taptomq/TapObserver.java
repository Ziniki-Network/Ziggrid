package org.ziggrid.taptomq;


public class TapObserver { /* implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger("Observer");
	private CouchbaseClient conn;
	private TapClient tap;
	static final int groupSize = 50;
	private static final String QUEUE_NAME = "tap";
	private String couchUrl;
	private String bucket;
	private int handledMessages;
	private int maxItems = -1;
	private String rabbitUrl;
	private Connection messageQueueConnection;
	private Channel queueChannel;

	public TapObserver(String couchUrl, String bucket, String rabbitUrl, int maxItems) {
		this.couchUrl = couchUrl;
		this.bucket = bucket;
		this.rabbitUrl = rabbitUrl;
		this.maxItems = maxItems;
	}

	public void run() {
		try {
			createInfrastructure();
			tap.tapBackfill("ziggridObserver_" + bucket, new Date().getTime() - 96400000, 0, TimeUnit.MINUTES);
			while (tap.hasMoreMessages()) {
				try {
					ResponseMessage msg = getNextMessage();
					if (msg == null)
						continue;
					if (!enqueueMessage(msg))
						continue;
					if (maxItems != -1 && maxItems-- <= 0) {
						logger.info("Reached maximum number of records");
						break;
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			close();
		}
	}

	public void createInfrastructure() {
		tap = openTap(couchUrl, bucket);
		try {
			messageQueueConnection = openAMQPConnection(rabbitUrl);
			queueChannel = createQueueChannel(messageQueueConnection);
		} catch (IOException e) {
			logger.error("Failure while trying to open AMQP connection/channel to " + rabbitUrl, e);
			System.exit(1);
		}
	}

	private Channel createQueueChannel(Connection messageQueueConnection) throws IOException {
		Channel queueChannel = messageQueueConnection.createChannel();
		queueChannel.queueDeclare(QUEUE_NAME, false, false, false, null);
		return queueChannel;
	}

	private Connection openAMQPConnection(String messageQueueUrl) throws IOException {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(messageQueueUrl);
		Connection connection = factory.newConnection();
		return connection;
	}

	public ResponseMessage getNextMessage() {
		ResponseMessage msg = tap.getNextMessage();
		if (msg != null) {
			handledMessages++;
			return msg;
		}

		logger.debug("Timed out waiting for next tap message");
		if (handledMessages > 0) {
			logger.info("Handled " + handledMessages + " tap messages");
			handledMessages = 0;
		}
		return null;
	}

	private boolean enqueueMessage(ResponseMessage msg) throws JSONException, IOException {
		String key = msg.getKey();
		String value = new String(msg.getValue());
		JSONObject obj = new JSONObject(value);
		if (!obj.has("ziggridType")) {
			logger.info("Ignoring object without ziggridType");
			return false;
		}
		String type = obj.getString("ziggridType");
		if (type == null)
			return false;

		TapDataPacket data = new TapDataPacket(key, value);
		queueChannel.basicPublish("", QUEUE_NAME, null, TapDataPacket.serialize(data));

		return true;
	}

	public CouchbaseClient openCouch(String couchUrl, String bucket) {
		try {
			logger.error("TAP Observer opening couchbase connection to " + bucket);
			URI server = new URI(couchUrl + "pools");
			List<URI> serverList = new ArrayList<URI>();
			serverList.add(server);
			CouchbaseConnectionFactoryBuilder builder = new CouchbaseConnectionFactoryBuilder();
			builder.setOpTimeout(30000);
			builder.setTimeoutExceptionThreshold(30000);
			CouchbaseConnectionFactory ccf = builder.buildCouchbaseConnection(serverList, bucket, "");
			return new CouchbaseClient(ccf);
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	public TapClient openTap(String couchUrl, String bucket) {
		try {
			URI server = new URI(couchUrl + "pools");
			List<URI> serverList = new ArrayList<URI>();
			serverList.add(server);
			return new TapClient(serverList, bucket, "");
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	public void close() {
		if (tap != null)
			tap.shutdown();
		if (conn != null) {
			logger.error("Shutting down TAP Observer");
			conn.shutdown();
		}
		try {
			if (messageQueueConnection != null)
				messageQueueConnection.close();
			if (queueChannel != null)
				queueChannel.close();
		} catch (IOException e) {
			logger.warn("Problems closing message queue connection/channel, e");
		}

	}
*/}
