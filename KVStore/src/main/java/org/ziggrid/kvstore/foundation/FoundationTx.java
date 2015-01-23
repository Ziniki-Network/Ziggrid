package org.ziggrid.kvstore.foundation;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.foundationdb.FDBException;
import com.foundationdb.KeyValue;
import com.foundationdb.Range;
import com.foundationdb.ReadTransaction;
import com.foundationdb.Transaction;
import com.foundationdb.async.AsyncIterable;
import com.foundationdb.async.AsyncIterator;
import com.foundationdb.async.Future;

public class FoundationTx {
	public class PropertyReaderIterator implements AsyncIterator<KeyValue> {
		private AsyncIterator<KeyValue> iterator;

		public PropertyReaderIterator(AsyncIterator<KeyValue> iterator) {
			this.iterator = iterator;
		}

		@Override
		public void cancel() {
			iterator.cancel();
		}

		@Override
		public void dispose() {
			iterator.dispose();
		}

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public KeyValue next() {
			KeyValue ret = iterator.next();
			synchronized (readProperties) {
				readProperties.add(hex(ret.getKey()));
			}
			return ret;
		}

		@Override
		public void remove() {
			iterator.remove();
		}

		@Override
		public Future<Boolean> onHasNext() {
			return iterator.onHasNext();
		}
	}

	public class PropertyReader implements AsyncIterable<KeyValue> {
		private final AsyncIterable<KeyValue> ret;

		public PropertyReader(AsyncIterable<KeyValue> ret) {
			this.ret = ret;
		}

		@Override
		public Future<List<KeyValue>> asList() {
			throw new RuntimeException("Not Implemented");
		}

		@Override
		public AsyncIterator<KeyValue> iterator() {
			return new PropertyReaderIterator(ret.iterator());
		}
	}

	private final Transaction tx;
	private final ReadTransaction snapshot;
	private final TreeSet<String> changedProperties = new TreeSet<String>();
	private final TreeSet<String> readProperties = new TreeSet<String>();
	private final HashSet<FoundationTx> concurrentTxs = new HashSet<FoundationTx>();
	private final String thread;
	private final int txno;
	private Set<FoundationTx> activeTransactions;
	private boolean triedToCommit = false;
	private boolean retryable;

	public FoundationTx(Transaction tx, int txno, Set<FoundationTx> activeTransactions) {
		this.tx = tx;
		this.activeTransactions = activeTransactions;
		this.snapshot = tx.snapshot();
		this.thread = Thread.currentThread().getName();
		this.txno = txno;
		synchronized (activeTransactions) {
			for (FoundationTx o : activeTransactions) {
				concurrent(o);
				o.concurrent(this);
			}
			activeTransactions.add(this);
		}
	}
	
	public boolean isRetryable() {
		return retryable;
	}

	public AsyncIterable<KeyValue> getSnapshotRange(byte[] from, byte[] to) {
		return snapshot.getRange(from, to);
	}

	public boolean containsValue(String id) {
		synchronized (readProperties) {
			readProperties.add(id);
		}
		return tx.get(id.getBytes()).get() != null;
	}

	public byte[] get(byte[] key) {
		synchronized (readProperties) {
			readProperties.add(hex(key));
		}
		return tx.get(key).get();
	}

	public String getString(String id) {
		synchronized (readProperties) {
			readProperties.add(id);
		}
		byte[] bs = tx.get(id.getBytes()).get();
		if (bs == null)
			return null;
		return new String(bs);
	}

	public AsyncIterable<KeyValue> getRange(byte[] from, byte[] to) {
		return new PropertyReader(tx.getRange(from, to));
	}
	
	public Map<String, String> getKeyRangePrefixed(String key, int top, boolean wantAscending) {
		byte[] bs = key.getBytes();
		if (top <= 0)
			top = Transaction.ROW_LIMIT_UNLIMITED;
		PropertyReader pr = new PropertyReader(tx.getRange(bs, fullRange(bs), top, !wantAscending));
		AsyncIterator<KeyValue> it = pr.ret.iterator();
		Map<String, String> ret = new LinkedHashMap<String, String>();
		while (it.hasNext()) {
			KeyValue next = it.next();
			ret.put(new String(next.getKey()), new String(next.getValue()));
		}
		return ret;
	}
	
	public void put(byte[] key, byte[] value) {
		tx.set(key, value);
		synchronized (readProperties) {
			changedProperties.add(hex(key));
		}
	}

	public void deletePrefixedWith(String id) {
		byte[] from = id.getBytes();
		tx.clear(new Range(from, fullRange(from)));
		synchronized (readProperties) {
			changedProperties.add(id);
		}
	}

	public void delete(byte[] id) {
		tx.clear(id);
		synchronized (readProperties) {
			changedProperties.add(hex(id));
		}
	}

	public void deleteKey(String id) {
		tx.clear(id.getBytes());
		synchronized (readProperties) {
			changedProperties.add(id);
		}
	}

	public static byte[] fullRange(byte[] from) {
		byte[] bs = new byte[from.length+1];
		for (int i=0;i<from.length;i++)
			bs[i] = from[i];
		bs[from.length] = (byte)0xff;
		return bs;
	}

	public void commit() {
		try {
			triedToCommit  = true;
			tx.commit().get();
		} catch (FDBException ex) {
			Future<Void> error = tx.onError(ex);
			retryable = !error.isError();
			if (ex.getCode() == 1020) {
				synchronized (activeTransactions) { // This is just here so that only one tx can fail completely at a time ...
					synchronized (concurrentTxs) {
						try {
							// Report about the failure
							System.err.println("Transaction failed: " + ex.toString());
							System.err.println("There were " + concurrentTxs.size() + " concurrent transactions on this node (there may be more in other processes)");
							System.err.println("Failed tx: " + this.dump());
							
							// Try and analyze the problem and give a succint explanation
							System.err.println("Analysis:");
							for (String s : readProperties)
								reportConflictsWith("read", s);
							for (String s : changedProperties)
								reportConflictsWith("changed", s);
							
							// Give full details on all conflicts
//							System.err.println("Concurrent Transactions:");
//							for (FoundationTx ctx : concurrentTxs)
//								System.err.println("  " + ctx.dump());
						} catch (Throwable t) {
							System.err.println("failed while diagnosing problems");
							t.printStackTrace();
						}
//						If all hell is about to break loose, and you're currently debugging, this is a good time to quit
//						System.exit(1);
						throw ex;
					}
				}
			}
		}
	}

	public void rollback() {
		tx.reset();
	}

	private void reportConflictsWith(String msg, String prop) {
		for (FoundationTx ctx : concurrentTxs) {
			synchronized (ctx.readProperties) {
				for (String s : ctx.readProperties) {
					if (prop.startsWith(s) || s.startsWith(prop))
						System.err.println("Transaction " + this.txno + " " + msg + " property " + prop + " while" + (!ctx.triedToCommit?" uncommitted":"") + " tx " + ctx.txno + " was reading " + s + (msg.equals("read")?" (shouldn't conflict)":""));
				}
				for (String s : ctx.changedProperties) {
					if (prop.startsWith(s) || s.startsWith(prop))
						System.err.println("Transaction " + this.txno + " " + msg + " property " + prop + " while" + (!ctx.triedToCommit?" uncommitted":"") + " tx " + ctx.txno + " was changing " + s);
				}
			}
		}
	}

	public String dump() {
		synchronized (readProperties) {
			return "FTX[" + thread + "#"+txno+"] R:"+readProperties+"C:"+changedProperties;
		}
	}

	public void concurrent(FoundationTx tx) {
		synchronized (concurrentTxs) {
			concurrentTxs.add(tx);
		}
	}

	public static String hex(byte[] b) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < b.length; i++)
			hex(sb, b[i] & 0xff);
		return sb.toString();
	}

	private static void hex(StringBuilder sb, int quant) {
		if (quant >= 32 && quant < 127) {
			sb.append((char)quant);
		} else {
			String s = Integer.toHexString(quant).toUpperCase();
			while (s.length() < 2)
				s = "0" + s;
			sb.append(s);
		}
	}
}
