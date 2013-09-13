package org.ziggrid.utils.http;

import java.io.File;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

public class GPServletDefn {
	private final String servletClass;
	private HttpServlet servletImpl;
	protected final GPServletConfig config;
	private String contextPath;
	private String servletPath;

	public GPServletDefn(InlineServer server, String servletClass) {
		config = new GPServletConfig(server, this);
		this.servletClass = servletClass;
		contextPath = "";
		servletPath = "";
	}

	public void setContextPath(String contextPath) {
		if (!contextPath.startsWith("/"))
			contextPath = "/" + contextPath;
		this.contextPath = contextPath;
		((GPServletContext)config.getServletContext()).setContextPath(contextPath);
	}

	public void setServletPath(String servletPath) {
		if (!servletPath.startsWith("/"))
			servletPath = "/" + servletPath;
		this.servletPath = servletPath;
		((GPServletContext)config.getServletContext()).setServletPath(servletPath);
	}

	public void init() throws ClassNotFoundException, InstantiationException, IllegalAccessException, ServletException {
		Class<?> forName = Class.forName(servletClass);
		InlineServer.logger.info("Creating servlet " + servletClass);
		servletImpl = (HttpServlet) forName.newInstance();
		servletImpl.init(config);
	}

	public void destroy() {
		InlineServer.logger.info("Destroying servlet " + servletClass);
		if (servletImpl != null) {
			servletImpl.destroy();
			servletImpl = null;
		}
	}

	public HttpServlet getImpl() {
		return servletImpl;
	}

	public void initParam(String key, String value) {
		config.initParam(key, value);
	}

	public boolean isForMe(String rawUri) {
		return rawUri.equals(contextPath+servletPath) || rawUri.startsWith(contextPath+servletPath+"/") || rawUri.startsWith(contextPath+servletPath+"?");
	}

	public GPServletConfig getConfig() {
		return config;
	}

	public void addClassDir(File dir) {
		config.getServletContext().addClassDir(dir);
	}

	public String getServletClass() {
		return servletClass;
	}
	
	@Override
	public String toString() {
		return servletClass;
	}
}
