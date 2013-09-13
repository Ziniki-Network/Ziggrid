package org.ziggrid.utils.sync;

import org.ziggrid.utils.exceptions.UtilException;

/** A choke point is a synchronization object which holds up any number of "waiting" threads
 * until a "master" thread releases the choke.
 *
 * <p>
 * &copy; 2013 Gareth Powell.  All rights reserved.
 *
 * @author Gareth Powell
 *
 */
public class ChokePoint {
	private boolean ready;
	private Object value;

	public void release() {
		synchronized (this) {
			ready = true;
			this.notifyAll();
		}
	}
	
	public void release(Object value) {
		if (ready)
			return;
		this.value = value;
		release();
	}
	
	public void hold() {
		while (true) {
			if (ready) return;
			SyncUtils.waitFor(this, 0);
		}
	}
	
	public Object getValue() {
		if (!ready)
			throw new UtilException("Cannot get value before ready");
		return value;
	}
}
