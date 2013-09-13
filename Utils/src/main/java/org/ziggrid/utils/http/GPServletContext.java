package org.ziggrid.utils.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.http.HttpServlet;

import org.ziggrid.utils.collections.IteratorEnumerator;
import org.ziggrid.utils.exceptions.UtilException;

public class GPServletContext implements ServletContext {
	private final GPServletConfig config;
	private Map<String, String> initParams = new HashMap<String, String>();
	private String contextPath = "";
	String servletPath = "";
	private Random rand = new Random();
	private Map<String, GPHttpSession> sessions = new HashMap<String, GPHttpSession>();
	private List<File> classdirs = new ArrayList<File>();
	
	public GPServletContext(GPServletConfig config) {
		this.config = config;
	}

	public void initParam(String key, String value) {
		initParams.put(key, value);
	}
	
	public void setContextPath(String path)
	{
		contextPath = path;
	}
	
	public void setServletPath(String path)
	{
		servletPath = path;
	}
	
	public void addClassDir(File dir)
	{
		classdirs.add(dir);
	}

	@Override
	public Object getAttribute(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServletContext getContext(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getContextPath() {
		return contextPath;
	}

	@Override
	public String getInitParameter(String s) {
		return initParams.get(s);
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		return new IteratorEnumerator<String>(initParams.keySet().iterator());
	}

	@Override
	public int getMajorVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getMimeType(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getMinorVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public RequestDispatcher getNamedDispatcher(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRealPath(String path) {
		if (path.startsWith("/WEB-INF/classes"))
		{
			if (classdirs.isEmpty())
				throw new UtilException("No class directories have been set in inline server: asked for " + path);
			String tmp = path.replace("WEB-INF/classes", "");
			if (tmp.startsWith("/"))
				tmp.replace("/", "");
			for (File dir : classdirs) {
				File ret = new File(dir, tmp);
				if (ret.exists())
					return ret.getAbsolutePath();
			}
			return null;
		}
		else
			throw new UtilException("Cannot resolve real path based on prefix: " + path);
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URL getResource(String arg0) throws MalformedURLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream getResourceAsStream(String s) {
		GPStaticResource ret = staticResource(s);
		if (ret == null)
			return null;
		return ret.stream;
	}

	GPStaticResource staticResource(String s) {
		for (File f : config.staticPaths(s)) {
			if (f.exists())
				try {
					if (f.isDirectory())
						f = new File(f, "index.html");
					if (f.exists())
						return new GPStaticResource(f, f.length(), new FileInputStream(f));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
		}
		InputStream ret = this.getClass().getResourceAsStream(s);
		if (ret != null)
			return new GPStaticResource(null, -1, ret);
		return null;
	}

	
	@Override
	public Set<String> getResourcePaths(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getServerInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Servlet getServlet(String arg0) throws ServletException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getServletContextName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Enumeration<String> getServletNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Enumeration<Servlet> getServlets() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void log(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void log(Exception arg0, String arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void log(String arg0, Throwable arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeAttribute(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setAttribute(String arg0, Object arg1) {
		// TODO Auto-generated method stub

	}

	public GPHttpSession newSession() {
		String cookie = Long.toHexString(rand.nextLong());
		GPHttpSession s = new GPHttpSession(this, cookie);
		sessions.put(cookie, s);
		return s;
	}

	public void deleteSession(GPHttpSession gpHttpSession) {
		loop:
			while (true)
			{
				for (String s : sessions.keySet())
					if (sessions.get(s).equals(gpHttpSession))
					{
						sessions.remove(s);
						continue loop;
					}
				return;
			}
	}
	
	public GPHttpSession getSession(String fromCookieValue)
	{
		return sessions.get(fromCookieValue);
	}

	@Override
	public Dynamic addFilter(String arg0, String arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Dynamic addFilter(String arg0, Filter arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Dynamic addFilter(String arg0, Class<? extends Filter> arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addListener(Class<? extends EventListener> arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addListener(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <T extends EventListener> void addListener(T arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0,
			String arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0,
			Servlet arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0,
			Class<? extends Servlet> arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends Filter> T createFilter(Class<T> arg0)
			throws ServletException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends EventListener> T createListener(Class<T> arg0)
			throws ServletException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends Servlet> T createServlet(Class<T> arg0)
			throws ServletException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void declareRoles(String... arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ClassLoader getClassLoader() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getEffectiveMajorVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getEffectiveMinorVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FilterRegistration getFilterRegistration(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JspConfigDescriptor getJspConfigDescriptor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServletRegistration getServletRegistration(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, ? extends ServletRegistration> getServletRegistrations() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SessionCookieConfig getSessionCookieConfig() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean setInitParameter(String arg0, String arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setSessionTrackingModes(Set<SessionTrackingMode> arg0)
			throws IllegalStateException, IllegalArgumentException {
		// TODO Auto-generated method stub
		
	}

	HttpServlet getThisServlet() {
		return config.getServlet();
	}
}
