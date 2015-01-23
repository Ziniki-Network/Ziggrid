package org.ziggrid.kvstore.foundation;

import java.util.Map;
import java.util.Map.Entry;

import org.ziggrid.kvstore.KVTransaction;
import org.ziggrid.kvstore.Sender;
import org.zinutils.exceptions.UtilException;
import org.zinutils.metrics.CodaHaleMetrics;

import com.codahale.metrics.Counter;

// This should be extracted to derive from a class it can share with FoundationKVStore
// The main purpose of this class is to handle creating queuing objects
public class FoundationSender implements Sender {
	private final byte[] unique = new byte[4];
	private final Thread createdIn;
	private int nextId = 0;
	
	public FoundationSender(short unique, short thrId) {
		createdIn = Thread.currentThread();
		this.unique[0] = (byte)((unique>>8)&0xff);
		this.unique[1] = (byte)(unique&0xff);
		this.unique[2] = (byte)((thrId>>8)&0xff);
		this.unique[3] = (byte)(thrId&0xff);
	}

	@Override
	public void queue(KVTransaction tx, String forProcessorSHA, byte[] itemId, Map<String,Object> fields) {
		if (createdIn != Thread.currentThread())
			throw new UtilException("Can only use sender in the thread in which it was created");
		Counter counter = CodaHaleMetrics.metrics.counter("queueCounter_" + forProcessorSHA);
//		System.out.println("itemId = " + StringUtil.hex(itemId) + " forProcessor = " + forProcessorSHA + " fields = " + fields);
		byte[] key = nextId(itemId);
//		System.err.println("Thread " + thrId + " with unique " + StringUtil.hex(unique) + " issuing id " + StringUtil.hex(key));
		tx.put(key, null, forProcessorSHA);
		for (Entry<String, Object> kv : fields.entrySet())
			tx.put(key, kv.getKey(), kv.getValue());
		counter.inc();
	}

	public byte[] nextId(byte[] itemId) {
		if (itemId.length != 20)
			throw new UtilException("SHA-1 ids should be 20 bytes");
		byte[] ret = new byte[1+20+2+2+4];
		ret[0] = 'q';
		for (int i=0;i<20;i++)
			ret[i+1] = itemId[i];
		for (int i=0;i<4;i++)
			ret[i+21] = unique[i];
		int msgId = nextId++;
		for (int i=0;i<4;i++)
			ret[i+25] = (byte) (msgId>>((3-i)*8) & 0xff);
		return ret;
	}
}
