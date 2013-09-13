package org.ziggrid.driver;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.utils.collections.ListMap;
import org.ziggrid.utils.collections.SetMap;
import org.ziggrid.utils.metrics.CodeHaleMetrics;
import org.ziggrid.utils.sync.SyncUtils;
import org.ziggrid.utils.utils.DateUtils;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;

public class BucketDelayLine<K,V> {
	private static final Logger logger = LoggerFactory.getLogger("Observer");
	
	private static final Meter entryProcessMeter = CodeHaleMetrics.metrics.meter("EntryProcessMeter");
	private static final Timer bucketProcessTimer = CodeHaleMetrics.metrics.timer("BucketProcessTimer");

	public interface Processor<T1, T2> {
		void spill(T1 k, Set<T2> set);
		void spill(T1 k, ListMap<T2, JSONObject> map);
		void allProcessed(Set<T1> keySet);
	}

	private class Bucket {
		
		private SetMap<K, V> entries = new SetMap<K, V>();
		
	}

	private class BucketProcessingThread extends Thread {
		private boolean drain;
		private int pause;

		public BucketProcessingThread(int pause) {
			super("BucketProcessingThread");
			this.pause = pause;
			setPriority(NORM_PRIORITY+1);
		}
		
		@Override
		public void run() {
			int counter = 0;
			int consecutiveEmpties = 0;
			Date nextBucket = new Date(new Date().getTime()+pause);
			while (buckets.size() > 0) {
				if (nextBucket.before(new Date()))
					logger.error("Failed to start bucket " + counter + " at " + DateUtils.Format.isodate.showDate(nextBucket));
				else
					SyncUtils.waitUntil(this, nextBucket);
				logger.debug("Entering bucket loop with " + buckets.size() + " active buckets");
				Bucket process;
				Map<K, ListMap<V, JSONObject>> immed;
				synchronized (buckets) {
					if (logger.isInfoEnabled()) {
						// Count up all the buckets and output the result
						TreeMap<String, Integer> counts = new TreeMap<String, Integer>();
						for (int i=0;i<buckets.size(); i++) {
							Bucket bucket = buckets.get(i);
							SetMap<K, V> entries = bucket.entries;
							for (K s : entries.keySet()) {
								int val = 0;
								if (counts.containsKey(s.toString()))
									val = counts.get(s.toString());
								counts.put(s.toString(), val+entries.size(s));
							}
						}
						for (Entry<K, ListMap<V, JSONObject>> kv : immediate.entrySet()) {
							K s = kv.getKey();
							int val = 0;
							if (counts.containsKey(s.toString()))
								val = counts.get(s.toString());
							counts.put(s.toString(), val+kv.getValue().keySet().size());
						}
						if (!counts.isEmpty())
							logger.info("Bucket Delay Line: " + counts);
					}
					++counter;
					process = buckets.remove(buckets.size()-1);
					if (!drain)
						buckets.add(0, new Bucket());
					immed = immediate;
					immediate = new HashMap<K, ListMap<V, JSONObject>>();
				}
				logger.debug("Bucket " + counter + " has " + process.entries.keySet().size() + " keys and " + immed.keySet().size() + " immediate keys");
				if (!process.entries.isEmpty() || !immed.isEmpty()) {
					logger.debug("Processing bucket " + counter + " with key set " + process.entries.keySet() + " and " + process.entries.totalSize() + " total entries; immediate = " + immed.keySet());
					final Timer.Context bucketProcessingContext = bucketProcessTimer.time();
					for (Entry<K, ListMap<V, JSONObject>> kv : immed.entrySet()) {
						try {
							logger.debug("Processing immediate key " + kv.getKey());
							observer.spill(kv.getKey(), kv.getValue());
						} catch (Exception ex) {
							logger.error("Error processing immediate entries ", ex);
						}
					}
					for (K k : process.entries) {
						try {
							observer.spill(k, process.entries.get(k));
							entryProcessMeter.mark(process.entries.size(k));
						} catch (Exception ex) {
							logger.error("Error processing buckets ", ex);
						}
					}
					logger.info("Finished processing bucket " + counter);
					bucketProcessingContext.stop();
					consecutiveEmpties = 0;
				} else {
					logger.debug("Bucket " + counter + " is empty");
					consecutiveEmpties++;
					if (consecutiveEmpties % 10 == 0)
						logger.info("Seen " + consecutiveEmpties + " consecutive empty buckets");
				}
				observer.allProcessed(process.entries.keySet());
				nextBucket = new Date(nextBucket.getTime() + pause);
			}
		}
	}
	
	private List<Bucket> buckets = new ArrayList<Bucket>();
	private BucketProcessingThread thr;
	private Processor<K, V> observer;

	private Map<K, ListMap<V, JSONObject>> immediate = new HashMap<K, ListMap<V, JSONObject>>();
	
	public BucketDelayLine(Processor<K,V> observer, int count, int pause) {
		this.observer = observer;
		for (int i=0;i<count;i++)
			buckets.add(new Bucket());
		thr = new BucketProcessingThread(pause);
		thr.start();
	}

	public void add(K key, V value) {
		synchronized (buckets) {
			buckets.get(0).entries.add(key, value);
		}
	}

	// obj should probably be parameterized
	public void doNext(K key, V value, JSONObject obj) {
		synchronized (buckets) { // because we change immediate, we synchronize on buckets everywhere to access it
			if (!immediate.containsKey(key))
				immediate.put(key, new ListMap<V, JSONObject>());
			immediate.get(key).add(value, obj);
		}
	}

	public void drain() {
		thr.drain = true;
		while (true) {
			try {
				thr.join();
				break;
			} catch (InterruptedException ex) {
				// no worries
			}
		}
	}
}
