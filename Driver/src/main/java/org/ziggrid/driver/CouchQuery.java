package org.ziggrid.driver;

import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.exceptions.ZiggridException;
import org.ziggrid.utils.utils.DateUtils;

import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.protocol.views.Query;
import com.couchbase.client.protocol.views.RowError;
import com.couchbase.client.protocol.views.Stale;
import com.couchbase.client.protocol.views.View;
import com.couchbase.client.protocol.views.ViewResponse;
import com.couchbase.client.protocol.views.ViewResponseNoDocs;
import com.couchbase.client.protocol.views.ViewRow;

public class CouchQuery {
	private static final Logger logger = LoggerFactory.getLogger("CouchQuery");
	private final CouchbaseClient conn;
	private final View view;

	public CouchQuery(CouchbaseClient conn, String documentName, String viewName) {
		this.conn = conn;
		this.view = viewName == null ? null : conn.getView(documentName, viewName);
	}
	
	/** Bump is supposed to ensure that the views are updated.
	 */
	public void bump() {
		if (view == null)
			return;
		Query q = new Query();
		q.setLimit(0);
		q.setStale(Stale.UPDATE_AFTER);
		logger.debug("Forcing view update for " + view.getViewName());
		query(q);
	}

	public ViewResponse query(final Query q) {
		if (view == null)
			throw new ZiggridException("No view specified");
		final Date from = new Date();
		try {
//			final Thread thr = Thread.currentThread();
			Timer t = new Timer();
			t.schedule(new TimerTask() {
				public void run() {
					logger.error("I would like to interrupt the query started at " + from + ": view = " + view.getViewName() + " with query = " + q);
//					thr.interrupt();
				}
			}, 5000, 1000);
			ViewResponse ret = conn.query(view, q);
			t.cancel();
			return ret;
		} catch (Exception ex) {
			logger.error("May have been interrupted", ex);
			return new ViewResponseNoDocs(new ArrayList<ViewRow>(), new ArrayList<RowError>());
		} finally {
			Date to = new Date();
			if (to.after(DateUtils.add(from, 4000)))
				logger.error("Query " + q + " on view " + view.getViewName() + " took " + DateUtils.elapsedTime(from, to, DateUtils.Format.sss3));
		}
	}

	public String getViewName() {
		return view.getViewName();
	}

	public CouchbaseClient getConnection() {
		return conn;
	}
}
