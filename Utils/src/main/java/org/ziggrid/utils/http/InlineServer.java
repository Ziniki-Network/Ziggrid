package org.ziggrid.utils.http;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.BindException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;

import org.ziggrid.utils.exceptions.UtilException;
import org.ziggrid.utils.utils.FileUtils;

public class InlineServer implements Runnable {
	public static final Logger logger = LoggerFactory.getLogger("InlineServer");

	private final RemoteIO remote;
	private final List<File> staticPaths = new ArrayList<File>();
	private final List<VirtualPath> virtualPaths = new ArrayList<VirtualPath>();
	
	// There is a list of servlets, but, by default, there is only one, the first in the list
	private final List<GPServletDefn> servlets = new ArrayList<GPServletDefn>();

	private final List<NotifyOnServerReady> interestedParties = new ArrayList<NotifyOnServerReady>();

	private boolean doLoop;

	private Throwable failure;

	private Thread inThread;

	private GPServletConfig staticConfig = new GPServletConfig(this, null);

	private long totalRequests;

	private int numRequests;

	private int exitStatus;

	public InlineServer(int port, String servletClass) {
		this.remote = new RemoteIO.UsingSocket(this, port);
		servlets.add(new GPServletDefn(this, servletClass));
		inThread = Thread.currentThread();
	}

	public InlineServer(String amqpUri, String servletClass) {
		this.remote = new RemoteIO.UsingAMQP(this, amqpUri);
		servlets.add(new GPServletDefn(this, servletClass));
		inThread = Thread.currentThread();
	}

	public GPServletDefn addServlet(String contextPath, String servletPath, String servletClass) {
		GPServletDefn defn = new GPServletDefn(this, servletClass);
		defn.setContextPath(contextPath);
		defn.setServletPath(servletPath);
		servlets.add(defn);
		return defn;
	}

	public void addFailure(Throwable ex) {
		if (failure == null)
			failure = ex;
	}

	public Throwable getFailure()
	{
		return failure;
	}

	public String getEndpoint() {
		return remote.getEndpoint();
	}
	
	public GPServletDefn getBaseServlet() {
		return servlets.get(0);
	}
	
	public void setContextPath(String path) {
		servlets.get(0).setContextPath(path);
	}

	public void setServletPath(String path) {
		servlets.get(0).setServletPath(path);
	}

	public void initParam(String key, String value) {
		servlets.get(0).initParam(key, value);
	}

	public void setAlert(String alert) {
		remote.setAlertTo(alert);
	}

	public void run() {
		run(true);
	}

	public boolean testPort() {
		try {
			remote.init();
		} catch (BindException ex) {
			return false;
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		} finally {
			try {
				remote.close();
			} catch (Exception ex) {
				throw UtilException.wrap(ex);
			}
		}
		return true;
	}
	public void run(boolean wantLoop) {
		if (inThread != Thread.currentThread())
			throw new UtilException("Cannot run in different thread to creation thread");
		logger.info("Starting execution of InlineServer " + this + " with wantLoop = " + wantLoop);
		try {
			this.doLoop = wantLoop;
			remote.init();
			for (GPServletDefn servlet : servlets)
				servlet.init();
			remote.announce(this.interestedParties);
			HashSet<ConnectionThread> threads = new HashSet<ConnectionThread>();
			while (doLoop) {
				logger.debug("Looking for connection on " + remote + " with doLoop = " + doLoop);
				RemoteIO.Connection conn = remote.accept();
				if (conn != null)
				{
					ConnectionThread thr = new ConnectionThread(this, conn);
					logger.debug("Accepting connection request and dispatching to thread " + thr);
					thr.start();
					threads.add(thr);
					for (ConnectionThread ct : threads)
					{
						if (!ct.isAlive())
						{
							threads.remove(ct);
							// It's good enough to break here after we've removed one, because if we remove at least one every time we add one it can't grow indefinitely ...
							break;
						}
					}
				}
			}

			logger.debug("doLoop = " + doLoop + " at end of loop, waiting for threads: " + threads);
			// Wait for all non-dead threads (at least for a while)
			try {
				for (ConnectionThread ct : threads)
				{
					if (ct.isAlive())
					{
						logger.info("Joining thread " + ct);
						ct.join(1000);
					}
				}
			} catch (InterruptedException ex) {
				logger.error("Interrupted waiting for threads to join ... trying again");
			}
			logger.info("Closing remote " + remote);
			remote.close();
		} catch (Exception ex) {
			if (ex.getClass().getName().equals("com.sun.jersey.api.container.ContainerException"))
				logger.error(ex.getMessage());
			else
				ex.printStackTrace();
			failure = ex;
			if (remote != null)
			{
				try {
					remote.close();
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}
		} finally {
			for (GPServletDefn servlet : servlets)
				servlet.destroy();
			logger.info("Server exiting");
			logger.info("Terminating execution of InlineServer " + this);
		}
	}

	public void service(GPRequest req, GPResponse resp) throws ServletException, IOException {
	}

	public void notify(NotifyOnServerReady toNotify) {
		interestedParties.add(toNotify);
	}

	public void pleaseExit() {
		pleaseExit(0);
	}

	public void pleaseExit(int status) {
		logger.info("InlineServer exit requested with status " + status);
		exitStatus = status;
		doLoop = false;
		
		// I claim this is not needed ... the accept times out and checks every 2s
//		inThread.interrupt();
	}
	
	public int getTerminationStatus() {
		return exitStatus;
	}

	public void addStaticDir(File file) {
		staticPaths.add(file);
	}

	public void addVirtualDir(String from, File file) {
		if (!from.startsWith("/"))
			from = "/"+from;
		if (!from.endsWith("/"))
			from += "/";
		virtualPaths.add(new VirtualPath(from, file));
	}


	public List<File> staticPaths(String s) {
		if (staticPaths.isEmpty() && virtualPaths.isEmpty())
			staticPaths.add(FileUtils.getCurrentDir().getAbsoluteFile());
		List<File> ret = new ArrayList<File>();
		for (File sp : staticPaths)
			ret.add(new File(sp, s));
		for (VirtualPath vp : virtualPaths) {
			if (s.startsWith(vp.vpath))
				ret.add(new File(vp.isTo, s.replace(vp.vpath, "")));
		}
		return ret;
	}

	public GPRequest requestFor(String s, InputStream is) throws URISyntaxException {
		String[] command = s.split(" ");
		String method = command[0];
		String rawUri = command[1];
		String protocol = command[2];

		for (GPServletDefn sd : servlets)
			if (sd.isForMe(rawUri)) {
				logger.debug("Choosing servlet " + sd);
				return new GPRequest(sd.getConfig(), method, rawUri, protocol, is);
			}

		logger.info("No servlet found for " + rawUri + "; looking for static file");
		return new GPRequest(staticConfig, method, rawUri, protocol, is);
	}

	public synchronized void requestTime(Date start, Date end) {
		long elapsed = end.getTime()-start.getTime();
		totalRequests += elapsed;
		numRequests++;
		logger.info("Request took " + (elapsed/1000.0) + "; total = " + totalRequests/1000.0 + "; average = " + totalRequests*10/numRequests/10000.0);
	}
}
