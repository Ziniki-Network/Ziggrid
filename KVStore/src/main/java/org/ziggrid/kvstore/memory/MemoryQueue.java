package org.ziggrid.kvstore.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ziggrid.kvstore.KVQueue;
import org.ziggrid.kvstore.KVTransaction;
import org.ziggrid.kvstore.QueuedItem;
import org.zinutils.sync.SyncUtils;
import org.zinutils.utils.StringUtil;

public class MemoryQueue extends KVQueue {
	private final List<QueuedItem> items = new ArrayList<QueuedItem>();

	public MemoryQueue() {
	}

	@Override
	public void startMessageFlow() {
	}

	public void put(String sha, byte[] itemId, Map<String, Object> fields) {
		QueuedItem qi = new QueuedItem(StringUtil.hex(itemId), sha);
		for (Entry<String, Object> f : fields.entrySet()) {
			qi.fields.put(f.getKey(), f.getValue());
		}
		synchronized (items) {
			items.add(qi);
			items.notify();
		}
	}
	
	@Override
	public QueuedItem nextMessage(KVTransaction tx) {
		synchronized (items) {
			while (items.isEmpty())
				SyncUtils.waitFor(items, 1000);
			return items.remove(0);
		}
	}

}
