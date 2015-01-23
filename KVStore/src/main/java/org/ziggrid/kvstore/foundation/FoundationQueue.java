package org.ziggrid.kvstore.foundation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.kvstore.KVQueue;
import org.ziggrid.kvstore.KVTransaction;
import org.ziggrid.kvstore.QueuedItem;
import org.zinutils.metrics.CodaHaleMetrics;
import org.zinutils.utils.StringUtil;

import com.codahale.metrics.Counter;
import com.foundationdb.KeyValue;
import com.foundationdb.async.AsyncIterable;

public class FoundationQueue extends KVQueue {
	protected static final Logger logger = LoggerFactory.getLogger("FQueue");
	private final byte[] qNameFrom;
	private final byte[] qNameTo;
	private byte[] readFrom = null;

	public FoundationQueue(byte[] from, byte[] to) {
		this.qNameFrom = from;
		this.qNameTo = to;
		
		logger.info("Reading queue partition from " + StringUtil.hex(qNameFrom) + " to " + StringUtil.hex(qNameTo));
	}

	@Override
	public void startMessageFlow() {
		// TODO Auto-generated method stub

	}

	@Override
	public QueuedItem nextMessage(KVTransaction tx) {
		if (readFrom == null) {
			logger.debug("Setting queue back to the start");
			readFrom = qNameFrom;
			
			// because of the snapshot issue, return null here ... may not be true
			return null;
		}
		byte[] from = readFrom;
		readFrom = null;
		logger.debug("Reading queue from " + StringUtil.hex(from) + " to " + StringUtil.hex(qNameTo));
		AsyncIterable<KeyValue> range = ((FoundationTx)tx).getSnapshotRange(from, qNameTo);
		byte[] prefix = null;
		QueuedItem ret = null;
		for (KeyValue x : range) {
			byte[] qItem = x.getKey();
			if (prefix == null) {
				prefix = qItem;
				// the value is the processor SHA
				String procSha = (String)ZiggridFTx.unmap(x.getValue());
				ret = new QueuedItem(StringUtil.hex(qItem), procSha);
				Counter counter = CodaHaleMetrics.metrics.counter("queueCounter_" + procSha);
				counter.dec();
			}
			else if (!beginsWith(qItem,prefix)) {
				readFrom = x.getKey();
				logger.debug("Setting lastRead to " + StringUtil.hex(readFrom));
				break;
			} else {
				String f = new String(qItem, prefix.length, qItem.length-prefix.length);
				ret.fields.put(f, ZiggridFTx.unmap(x.getValue()));
			}
			logger.debug("Deleting item " + StringUtil.hex(qItem) + " from queue " + StringUtil.hex(from));
			tx.delete(qItem);
		}
		if (ret != null || readFrom == null)
			return ret;

		// next time try from the beginning of the queue space
		// BECAUSE we use a snapshot range, our deletes don't show up, so we can't do this straight away
		readFrom = null;
		logger.debug("Nothing left in queue - reset next time");
		return null;
	}

	private boolean beginsWith(byte[] actual, byte[] prefix) {
		if (actual.length < prefix.length)
			return false;
		for (int i=0;i<prefix.length;i++)
			if (actual[i] != prefix[i])
				return false;
		return true;
	}

	@Override
	public String toString() {
		return "queue["+StringUtil.hex(qNameFrom)+"]";
	}
}
