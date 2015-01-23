package org.ziggrid.kvstore.foundation;

import java.util.HashSet;
import java.util.Set;

import org.ziggrid.kvstore.KVDatabase;
import org.ziggrid.kvstore.KVQueue;
import org.ziggrid.kvstore.KVTransaction;
import org.zinutils.exceptions.UtilException;

import com.foundationdb.Database;
import com.foundationdb.Transaction;
import com.foundationdb.async.Function;

public class FoundationDatabase implements KVDatabase {
	private final Database db;
	private final short unique;
	private int txno = 1;
	private short nextThreadId = 1;
	private Set<FoundationTx> activeTransactions = new HashSet<FoundationTx>();
	private final ThreadLocal<ZiggridFTx> currentTx = new ThreadLocal<ZiggridFTx>();
	private final ThreadLocal<FoundationSender> threadSender = new ThreadLocal<FoundationSender>();

	public FoundationDatabase(Database db, short unique) {
		this.db = db;
		this.unique = unique;
	}

	@Override
	public synchronized ZiggridFTx beginTx() {
		return wrap(db.createTransaction());
	}
	
	public ZiggridFTx wrap(Transaction tx) {
		synchronized (activeTransactions) {
			if (currentTx.get() != null)
				throw new UtilException("Cannot create a second transaction in " + Thread.currentThread());
			
			FoundationSender sender = threadSender.get();
			if (sender == null) {
				sender = new FoundationSender(unique, nextThreadId++);
				threadSender.set(sender);
			}
			ZiggridFTx ret = new ZiggridFTx(this, sender, txno++, tx, activeTransactions);
			currentTx.set(ret);
			return ret;
		}
	}

	public void unwrap(ZiggridFTx ftx) {
		cleanupTx(ftx);
	}

	@Override
	public <T> T run(final Function<KVTransaction, T> function) {
		return db.run(new Function<Transaction, T>() {
			@Override
			public T apply(Transaction tx) {
				return function.apply(wrap(tx));
			}
			
		});
	}

	public void cleanupTx(ZiggridFTx tx) {
		synchronized (activeTransactions) {
			activeTransactions.remove(tx);
			currentTx.set(null);
		}
	}

	@Override
	public KVQueue queueFrom(int fromQ, int toQ, int range) {
		byte[] qf, qt;
		if (range == 256) {
			qf = new byte[] { 'q', (byte) fromQ };
			qt = new byte[] { 'q', (byte) toQ };
		} else {
			qf = new byte[] { 'q', (byte) (fromQ/256), (byte)(fromQ%256) };
			qt = new byte[] { 'q', (byte) (toQ/256), (byte)(toQ%256) };
		}
		if (toQ == range) {
			// when we want the final range, we have to cover all the cases, thus 'r'
			qt = new byte[] { 'r' };
		}
		return new FoundationQueue(qf, qt);
	}

}
