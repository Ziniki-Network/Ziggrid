package org.ziggrid.driver;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.tapmessage.ResponseMessage;

import org.ziggrid.taptomq.TapDataPacket;
import org.ziggrid.utils.exceptions.UtilException;

import com.couchbase.client.TapClient;

public class RawTapMessageSource implements MessageSource {

	private TapClient tap;
	private String couchUrl;
	private String bucket;
	
	public RawTapMessageSource(String couchUrl, String bucket, Object... opts) {
		this.couchUrl = couchUrl;
		this.bucket = bucket;
	}

	@Override
	public void initialize() {
		tap = openTap(couchUrl, bucket);
	}
	
	public TapClient openTap(String couchUrl, String bucket) {
		try {
			URI server = new URI(couchUrl+"pools");
			List<URI> serverList = new ArrayList<URI>();
			serverList.add(server);
			return new TapClient(serverList, bucket, "");
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	@Override
	public TapDataPacket getNextMessage() {
		ResponseMessage message = tap.getNextMessage();
		if(message == null)
			return null;
		return new TapDataPacket(message.getKey(), new String(message.getValue()));
	}

	@Override
	public void startMessageFlow() throws Exception {
		tap.tapBackfill("ziggridObserver_"+bucket, new Date().getTime()-96400000, 0, TimeUnit.MINUTES);
	}

	@Override
	public boolean hasMoreMessages() {
		return tap.hasMoreMessages();
	}

	@Override
	public void shutdown() {
		if (tap != null)
			tap.shutdown();
	}
}
