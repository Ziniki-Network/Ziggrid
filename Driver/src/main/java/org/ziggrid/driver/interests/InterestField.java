package org.ziggrid.driver.interests;

import java.util.HashSet;
import java.util.Set;

import org.zinutils.collections.SetMap;

public class InterestField {
	private final Set<Interest> caresAbout = new HashSet<Interest>();
	private final SetMap<Object, Interest> valueWanted = new SetMap<Object, Interest>();

	public synchronized void matches(Object value, Interest i) {
		caresAbout.add(i);
		valueWanted.add(value, i);
	}
	
	public synchronized void remove(Interest i) {
		caresAbout.remove(i);
		for (Object o : valueWanted)
			valueWanted.remove(o, i);
	}

	public synchronized void qualify(Set<Interest> ret, Set<Interest> rejected, Object o) {
		ret.removeAll(caresAbout);
		Set<Interest> toReject = new HashSet<Interest>(caresAbout);
		if (o != null && valueWanted.contains(o)) {
			Set<Interest> couldAdd = valueWanted.get(o);
			for (Interest i : couldAdd) {
				toReject.remove(i);
				if (!rejected.contains(i))
					ret.add(i);
			}
		}
		rejected.addAll(toReject);
	}
}
