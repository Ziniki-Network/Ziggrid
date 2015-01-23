package org.ziggrid.driver;

import java.util.Map;
import java.util.Set;

import org.ziggrid.model.Model;

public interface ZiggridObserver extends Runnable {

	/** Obtain the Model being observed
	 * @return the Model definition
	 */
	Model getModel();

	/** Get a list of all the items this observer is potentially spitting out to be watched
	 * @return a list of the watchable names
	 */
	Set<String> getWatchables();

	/** Start watching a particular table for particular events
	 * @param lsnr the listener to receive the events
	 * @param obs the unique id of the observation
	 * @param table the name of the table to listen to
	 * @param constraints a set of name/value pairs to constrain the matching
	 * @return true if there were any matching observations possible
	 */
	boolean watch(ObserverListener lsnr, int obs, String table, Map<String, Object> constraints);

	/** Stop watching a specific observation
	 * @param obs the observation handle passed in to watch
	 */
	void unwatch(int hs);

	/** Shut down the observer
	 */
	void close();

}
