package org.ziggrid.generator.main;

import org.ziggrid.utils.utils.StringUtil;

public class Timer {
	public class Tick {
		private final int when;

		public Tick(int when) {
			this.when = when;
		}

		@Override
		public String toString() {
			return "["+StringUtil.digits(when/1000, 6) + ".---"+"]";
		}
	}

	public final static class Timestamp {
		private final int when;

		public Timestamp(int when) {
			this.when = when;
		}
		
		public int wholes() {
			return when/1000; 
		}
		
		public int millis() {
			return when%1000;
		}

		@Override
		public String toString() {
			return "["+StringUtil.digits(when/1000, 6) + "."+StringUtil.digits(when%1000, 3)+"]";
		}
	}

	private final boolean realtime;
	private int current;

	public Timer(boolean realtime, int initial) {
		this.realtime = realtime;
		current = initial*1000;
	}

	public boolean lessThan(int endAt) {
		return current < endAt;
	}

	public void next() {
		advanceTo(((current + 1000) / 1000)*1000);
	}

	public Timestamp current() {
		return new Timestamp(current);
	}
	
	public Timestamp notch() {
		Timestamp ret = new Timestamp(current);
		advanceTo(current+1);
		return ret;
	}

	private void advanceTo(int next) {
		if (realtime)
			try {
				Thread.sleep(next-current);
			} catch (InterruptedException ex) {
				// no matter, I guess
			}
			
		current = next;
	}

	@Override
	public String toString() {
		return new Timestamp(current).toString();
	}

	public Tick future(int i) {
		if (i == 0)
			throw new RuntimeException("0s is not really in the future");
		return new Tick((current/1000+i)*1000);
	}

	public boolean isNow(Tick tick) {
		return current / 1000 == tick.when/1000;
	}
}
