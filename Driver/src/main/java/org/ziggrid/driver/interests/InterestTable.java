package org.ziggrid.driver.interests;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.ziggrid.api.StoreableObject;

/** This is a nested data structure intended to make for very fast
 * analysis of whether an object is "interesting".
 * 
 * There is one such object for each observable table in the system.
 * When an object is ready to be observed, the appropriate table for its type is consulted,
 * and the list of "interestedParties" is assumed to be the default.
 * 
 * This is then filtered by any required options in a repeating process of looking at
 * each field and whittling down who is interested.
 * 
 * At the end of the day, there should be a list of interested parties.
 *
 * <p>
 * &copy; 2014 Gareth Powell.  All rights reserved.
 *
 * @author Gareth Powell
 *
 */
public class InterestTable {
	private final Set<Interest> interestedParties = new HashSet<Interest>();
	private final Map<String, InterestField> fields = new HashMap<String, InterestField>();

	public synchronized void addInterested(Interest i) {
		interestedParties.add(i);
		i.feeds(this);
	}
	
	public synchronized void removeInterest(Interest i) {
		interestedParties.remove(i);
		for (InterestField f : fields.values())
			f.remove(i);
	}

	public synchronized void requireField(String key, Object value, Interest i) {
		if (!fields.containsKey(key))
			fields.put(key, new InterestField());
		InterestField f = fields.get(key);
		f.matches(value, i);
	}

	public synchronized Set<Interest> qualify(StoreableObject so) {
		Set<Interest> ret = new HashSet<Interest>(interestedParties);
		Set<Interest> rejected = new HashSet<Interest>();
		for (Entry<String, InterestField> f : fields.entrySet()) {
			f.getValue().qualify(ret, rejected, so.get(f.getKey()));
		}
		return ret;
	}

}
