package org.ziggrid.kvstore.memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.codehaus.jettison.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.api.StoreableObject;
import org.ziggrid.kvstore.KVDatabase;
import org.ziggrid.kvstore.KVQueue;
import org.ziggrid.kvstore.KVTransaction;

import com.foundationdb.async.Function;

/** The in-memory database consists of two separate collections of information.
 * The "contents" represents a mapping of ID->Object
 * The "queues" represents a set of buckets, each bucket having a list of keys
 *
 * <p>
 * &copy; 2013 Gareth Powell.  All rights reserved.
 *
 * @author Gareth Powell
 *
 */
public class MemoryDatabase implements KVDatabase {
	public static Logger logger = LoggerFactory.getLogger("MemoryDB");
	private final ThreadLocal<MemorySender> threadSender = new ThreadLocal<MemorySender>();
	final TreeMap<String, Object> contents = new TreeMap<String, Object>();
	final List<MemoryQueue> queues = new ArrayList<MemoryQueue>();
	final HashMap<Integer, MemoryQueue> queuesByKey = new HashMap<Integer, MemoryQueue>();
	private int queueRange;

	public <T> T run(Function<KVTransaction, T> function) {
		MemoryTx t = beginTx();
		T ret = function.apply(t);
		t.commit();
		return ret;
	}
	
	public void dump() {
		for (Entry<String, Object> e : contents.entrySet())
			try {
				Object value = e.getValue();
				String str;
				if (value == null)
					str = "null";
				else if (value instanceof StoreableObject)
					str = ((StoreableObject) value).asJsonString();
				else
					str = value.toString();
				System.out.println(e.getKey() + "=>"+str);
			} catch (JSONException ex) {
				ex.printStackTrace();
			}
		for (MemoryQueue q : queues) {
			System.out.println("Q: " + q);
		}
	}

	public void dispose() {
		contents.clear();
	}

	
	@Override
	public KVQueue queueFrom(int fromQ, int toQ, int range) {
		logger.info("Creating queue from " + fromQ + " to " + toQ + " in " + range);
		MemoryQueue mq = new MemoryQueue();
		queues.add(mq);
		for (int i=fromQ;i<toQ;i++)
			queuesByKey.put(i, mq);
		if (range == 256)
			queueRange = 1;
		else
			queueRange = 2;
		return mq;
	}

	@Override
	public MemoryTx beginTx() {
		MemorySender sender = threadSender.get();
		if (sender == null) {
			sender = new MemorySender(this);
			threadSender.set(sender);
		}
		return new MemoryTx(this, sender);
	}

	public MemoryQueue getQueue(byte[] itemId) {
		int which;
		if (queueRange == 1)
			which = (itemId[0] & 0xff);
		else
			which = ((itemId[0]<<8) | (itemId[1])) & 0xffff;
		return queuesByKey.get(which);
	}
}
