package org.ziggrid.kvstore.foundation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.api.StoreableObject;
import org.ziggrid.kvstore.KVTransaction;
import org.ziggrid.kvstore.Sender;
import org.zinutils.exceptions.UtilException;

import com.foundationdb.KeyValue;
import com.foundationdb.Transaction;
import com.foundationdb.async.AsyncIterable;

public class ZiggridFTx extends FoundationTx implements KVTransaction {
	protected static final Logger logger = LoggerFactory.getLogger("FTx");
	private final FoundationDatabase db;
	private final Set<StoreableObject> updatedObjects = new HashSet<StoreableObject>();
	private final FoundationSender sender;

	public ZiggridFTx(FoundationDatabase kvdb, FoundationSender sender, int txno, Transaction tx, Set<FoundationTx> activeTransactions) {
		super(tx, txno, activeTransactions);
		this.db = kvdb;
		this.sender = sender;
	}

	@Override
	public Object getValue(String id) {
		byte[] bs = super.get(id.getBytes());
		if (bs == null)
			return null;
		return unmap(bs);
	}
	
	@Override
	public StoreableObject get(String id) {
		String type = null;
		LinkedHashMap<String, Object> fields = new LinkedHashMap<String, Object>();
		int skiplen = id.length() + 1;
		byte[] from = (id+".").getBytes();
		byte[] to = fullRange(from);
		boolean found = false;
		for (KeyValue kv : getRange(from, to)) {
			found = true;
			String key = new String(kv.getKey()).substring(skiplen);
//			logger.info("Recovered key " + key);
			if (key.equals("id"))
				continue;
			if (key.equals("ziggridType"))
				type = (String) unmap(kv.getValue());
			else
				fields.put(key, unmap(kv.getValue()));
		}
//		logger.info("Done recovering object");
		if (!found)
			return null;
		return new StoreableObject(type, id, fields);
	}

	@Override
	public List<StoreableObject> getAfter(String afterId, String lastId) {
		return getObjects(afterId.getBytes(), lastId.getBytes());
	}

	@Override
	public List<StoreableObject> getAllPrefixed(String prefix) {
		byte[] bs = prefix.getBytes();
		return getObjects(bs, fullRange(bs));
	}

	public List<StoreableObject> getObjects(byte[] from, byte[] to) {
		AsyncIterable<KeyValue> range = getRange(from, to);
		List<StoreableObject> objs = new ArrayList<StoreableObject>();
		String cid = null;
		String type = null;
		LinkedHashMap<String, Object> fields = null;
		for (KeyValue kv : range) {
			String key = new String(kv.getKey());
			int idx = key.lastIndexOf('.');
			String id = key.substring(0, idx);
			String fname = key.substring(idx+1);
			if (cid == null || !cid.equals(id)) {
				if (cid != null) {
					StoreableObject so = new StoreableObject(type, cid, fields);
					objs.add(so);
				}
				cid = id;
				fields = new LinkedHashMap<String, Object>();
			}
			if (fname.equals("id"))
				;
			else if (fname.equals("ziggridType"))
				type = (String)unmap(kv.getValue());
			else
				fields.put(fname, unmap(kv.getValue()));
		}
		if (cid != null) {
			StoreableObject so = new StoreableObject(type, cid, fields);
			objs.add(so);
		}
		return objs;
	}

	@Override
	public boolean contains(String id) {
		byte[] from = (id + ".").getBytes();
		return super.getRange(from, fullRange(from)).iterator().hasNext();
	}

	@Override
	public void require(String id, String ofType) {
		set(id+".id", id);
		if (ofType != null)
			set(id+".ziggridType", ofType);
	}

	@Override
	public void setField(String id, String key, Object value) {
		set(id+"." + key, value);
	}

	@Override
	public void put(String key, String str) {
		// For some reason, this does NOT use mapBytes
		super.put(key.getBytes(), str.getBytes());
	}

	@Override
	public void putValue(String string, Object val) {
		super.put(string.getBytes(), mapbytes(val));
	}

	@Override
	public void put(byte[] queue, String f, Object val) {
		byte[] fb;
		if (f == null)
			fb = new byte[0];
		else
			fb = f.getBytes();
		byte[] key = new byte[queue.length + fb.length];
		for (int i=0;i<queue.length;i++)
			key[i] = queue[i];
		for (int i=0;i<fb.length;i++)
			key[i+queue.length]= fb[i];
		super.put(key, mapbytes(val));
	}

	@Override
	public void put(String id, StoreableObject obj) {
		for (String f : obj.keys()) {
			set(id+"."+f, obj.get(f));
		}
		updatedObjects.add(obj);
	}

	@Override
	public void deleteObject(String id) {
		super.deletePrefixedWith(id + ".");
	}

	private void set(String key, Object value) {
		super.put(key.getBytes(), mapbytes(value));
	}

	public static byte[] mapbytes(Object o) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			if (o == null)
				; // do nothing, length == 0
			else if (o instanceof Boolean) {
				boolean b = (Boolean) o;
				if (b)
					dos.write(1);
				else
					dos.write(0);
			} else if (o instanceof String) {
				dos.write(2);
				dos.write(((String)o).getBytes());
			} else if (o instanceof Integer) {
				dos.write(3);
				dos.writeInt((Integer) o);
			} else if (o instanceof Long) {
				dos.write(4);
				dos.writeLong((Long) o);
			} else if (o instanceof Double) {
				dos.write(6);
				dos.writeDouble((Double) o);
			} else
				throw new UtilException("Cannot write object of type " + o.getClass());
			dos.flush();
			return baos.toByteArray();
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	public static Object unmap(byte[] bs) {
		if (bs == null || bs.length == 0)
			return null;
		try {
			int code = bs[0];
			if (code == 0)
				return false;
			else if (code == 1)
				return true;
			else if (code == 2) {
				return new String(bs, 1, bs.length-1);
			} else {
				ByteArrayInputStream bais = new ByteArrayInputStream(bs, 1, bs.length);
				DataInputStream dis = new DataInputStream(bais);
				if (code == 3)
					return dis.readInt();
				else if (code == 4)
					return dis.readLong();
				else if (code == 6)
					return dis.readDouble();
				else
					throw new UtilException("Cannot identify code: " + code);
			}
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	@Override
	public Collection<StoreableObject> updatedObjects() {
		return updatedObjects;
	}
	
	@Override
	public Sender getSender() {
		return sender;
	}

	@Override
	public void commit() {
		try {
			super.commit();
		} finally {
			db.cleanupTx(this);
		}
	}

	@Override
	public void rollback() {
		try {
			super.rollback();
		} finally {
			db.cleanupTx(this);
		}
	}
}
