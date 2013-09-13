package org.ziggrid.utils.sync;

import java.util.Date;

public class SyncUtils {

	public static boolean waitFor(Object waitOn, int ms) {
		if (ms == 0)
			return waitUntil(waitOn, null);
		else {
			Date d = new Date();
			d = new Date(d.getTime() + ms);
			return waitUntil(waitOn, d);
		}
	}

	/**
	 * 
	 * @param waitOn the object to wait on
	 * @param until when to wait until, or null to wait forever
	 * @return true if signalled before the time is up
	 */
	public static boolean waitUntil(Object waitOn, Date until) {
		if (until != null && new Date().after(until))
			return false;
		synchronized (waitOn) {
			while (until == null || new Date().before(until)) {
				try {
					long timeout = until == null ? 0 : until.getTime()-new Date().getTime();
					waitOn.wait(timeout);
					return until == null || new Date().before(until);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}

	public static void sleep(int ms) {
		Date end = new Date();
		end = new Date(end.getTime() + ms);
		Date curr;
		while ((curr = new Date()).before(end)) {
			try {
				Thread.sleep(end.getTime() - curr.getTime());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static void join(Thread thr) {
		while (thr.isAlive()) {
			try {
				thr.join();
			} catch (InterruptedException ex) {
				// no worries
			}
		}
	}

	public static ChokePoint chokePoint() {
		return new ChokePoint();
	}

}
