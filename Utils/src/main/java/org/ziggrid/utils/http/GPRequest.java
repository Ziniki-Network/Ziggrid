package org.ziggrid.utils.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.ziggrid.utils.collections.IteratorEnumerator;
import org.ziggrid.utils.collections.ListMap;
import org.ziggrid.utils.exceptions.UtilException;
import org.ziggrid.utils.http.ws.InlineServerWebSocketHandler;
import org.ziggrid.utils.utils.FileUtils;

public class GPRequest implements HttpServletRequest {

	static final Logger logger = LoggerFactory.getLogger("InlineServer");
	private final String method;
	private final URI uri;
	private final ListMap<String, String> headers = new ListMap<String, String>();
	private final GPServletContext context;
	private GPResponse response;
	private final String rawUri;
	private final InputStream is;
	private GPServletInputStream servletInputStream;
	private GPHttpSession session;
	private final List<Cookie> cookies = new ArrayList<Cookie>();
	private Cookie[] cookieArr;
	private Map<String, Object> attributes = new HashMap<String, Object>();
//	private GPAsyncContext async;
	private HashMap<String, String[]> parameterMap;
	private String encoding = "iso-8859-1";
	private BufferedReader reader = null;
	InlineServerWebSocketHandler wshandler;
	private final String protocol;

	public GPRequest(GPServletConfig config, String method, String rawUri, String protocol, InputStream is) throws URISyntaxException {
		this.protocol = protocol;
		this.context = config.getServletContext();
		this.method = method;
		this.rawUri = rawUri;
		this.is = is;
		uri = new URI(rawUri);
		logger.debug(Thread.currentThread().getName()+ ": " + "Received " + method + " request for " + rawUri);
		logger.debug("Created request with servlet " + this.getServlet());
	}

	public void addHeader(String s) {
		int colon = s.indexOf(":");
		if (colon == -1)
			return;
		String hdr = s.substring(0, colon).toLowerCase();
		String value = s.substring(colon+1).trim();
		headers.add(hdr, value);
		if (hdr.equals("cookie"))
		{
			for (String c : value.split(";")) {
				String[] c1 = c.trim().split("=");
				Cookie cookie = new Cookie(c1[0], c1[1]);
				cookies.add(cookie);
				if (cookie.getName().equals("JSESSIONID"))
				{
					session = context.getSession(cookie.getValue());
					if (session != null)
						session.accessed();
				}
			}
		}
	}
	
	public void endHeaders() {
		if (headers.contains("content-length"))
			servletInputStream = new GPServletInputStream(is, getIntHeader("content-length"));
		else
			servletInputStream = new GPServletInputStream(is, -1);
	}

	@Override
	public Object getAttribute(String key) {
		return attributes.get(key);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return new IteratorEnumerator<String>(attributes.keySet().iterator());
	}

	@Override
	public void removeAttribute(String key) {
		attributes.remove(key);
	}

	@Override
	public void setAttribute(String key, Object value) {
		attributes.put(key, value);
	}

	@Override
	public String getCharacterEncoding() {
		return encoding;
	}

	@Override
	public int getContentLength() {
		return Integer.parseInt(getHeader("Content-Length"));
	}

	@Override
	public String getContentType() {
		return this.getHeader("Content-Type");
	}

	@Override
	public GPServletInputStream getInputStream() throws IOException {
		return servletInputStream;
	}

	@Override
	public String getLocalAddr() {
		throw new UtilException("Not implemented");
	}

	@Override
	public String getLocalName() {
		throw new UtilException("Not implemented");
	}

	@Override
	public int getLocalPort() {
		throw new UtilException("Not implemented");
	}

	@Override
	public Locale getLocale() {
		throw new UtilException("Not implemented");
	}

	@Override
	public Enumeration<Locale> getLocales() {
		throw new UtilException("Not implemented");
	}

	@Override
	public String getParameter(String arg0) {
		String q = uri.getQuery();
		if (q == null)
			return null;
		int k = q.indexOf(arg0 + "=");
		if (k == 0 || (k > 0 && q.charAt(k-1) == '&'))
		{
			int from = k+arg0.length()+1;
			int t = q.indexOf('&', from);
			if (t == -1)
				return q.substring(from);
			else
				return q.substring(from, t);
		}
		return null;
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		if (parameterMap != null)
			return parameterMap;
		String q = uri.getQuery();
		ListMap<String, String> tmp = new ListMap<String, String>();
		if (q != null)
			analyzeQueryString(tmp, q, false);
		if (getContentType() != null && getContentType().startsWith("application/x-www-form-urlencoded"))
			try {
				analyzeQueryString(tmp, FileUtils.readNStream(getContentLength(), getInputStream()), true);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		parameterMap = new HashMap<String, String[]>();
		for (String s : tmp.keySet())
		{
			List<String> list = tmp.get(s);
			parameterMap.put(s, list.toArray(new String[list.size()]));
		}
		return parameterMap;
	}

	private void analyzeQueryString(ListMap<String, String> tmp, String q, boolean needsDecode) {
		int k=0;
		while (k < q.length())
		{
			int p = k;
			while (k < q.length() && q.charAt(k) != '=' && q.charAt(k) != '&')
				k++;
			String s = q.substring(p, k);
			if (k == q.length() || q.charAt(k) == '&')
			{
				tmp.add(s, "");
				continue;
			}
			int v = ++k; // skip the =
			while (k < q.length() && q.charAt(k) != '&')
				k++;
			String r = q.substring(v, k);
			if (needsDecode)
				tmp.add(s, urlDecode(r));
			else
				tmp.add(s, r);
			k++; // skip the &
		}
	}

	private String urlDecode(String s) {
		try {
			return URLDecoder.decode(s, "UTF-8");
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	@Override
	public Enumeration<String> getParameterNames() {
		return new IteratorEnumerator<String>(getParameterMap().keySet().iterator());
	}

	@Override
	public String[] getParameterValues(String arg0) {
		getParameterMap();
		if (!parameterMap.containsKey(arg0))
			return null;
		return parameterMap.get(arg0);
	}

	@Override
	public String getProtocol() {
		return protocol;
	}

	@Override
	public BufferedReader getReader() throws IOException {
		if (reader == null)
			reader = new BufferedReader(new InputStreamReader(servletInputStream, encoding));
		return reader;
	}

	@Override
	public String getRealPath(String arg0) {
		throw new UtilException("Not implemented");
	}

	@Override
	public String getRemoteAddr() {
		throw new UtilException("Not implemented");
	}

	@Override
	public String getRemoteHost() {
		throw new UtilException("Not implemented");
	}

	@Override
	public int getRemotePort() {
		throw new UtilException("Not implemented");
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String arg0) {
		throw new UtilException("Not implemented");
	}

	@Override
	public String getScheme() {
		throw new UtilException("Not implemented");
	}

	@Override
	public String getServerName() {
		throw new UtilException("Not implemented");
	}

	@Override
	public int getServerPort() {
		throw new UtilException("Not implemented");
	}

	@Override
	public boolean isSecure() {
		throw new UtilException("Not implemented");
	}

	@Override
	public void setCharacterEncoding(String arg0)
			throws UnsupportedEncodingException {
		throw new UtilException("Not implemented");
	}

	@Override
	public String getAuthType() {
		throw new UtilException("Not implemented");
	}

	@Override
	public String getContextPath() {
		return context.getContextPath();
	}

	@Override
	public Cookie[] getCookies() {
		if (cookieArr == null)
		{
			cookieArr = cookies.toArray(new Cookie[cookies.size()]);
		}
		return cookieArr;
	}

	@Override
	public long getDateHeader(String arg0) {
		throw new UtilException("Not implemented");
	}

	@Override
	public String getHeader(String s) {
		if (!headers.contains(s.toLowerCase()))
			return null;
		return headers.get(s.toLowerCase()).get(0);
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		return new IteratorEnumerator<String>(headers.iterator());
	}

	@Override
	public Enumeration<String> getHeaders(String s) {
		if (!headers.contains(s.toLowerCase()))
			return new Vector<String>().elements();
		return new IteratorEnumerator<String>(headers.get(s.toLowerCase()).iterator());
	}

	@Override
	public int getIntHeader(String arg0) {
		if (!headers.contains(arg0.toLowerCase()))
			return 0;
		return Integer.parseInt(headers.get(arg0.toLowerCase()).get(0));
	}

	@Override
	public String getMethod() {
		return method;
	}

	@Override
	public String getPathInfo() {
		String up = uri.getPath();
		String ret = up.replace(getContextPath(), "");
		ret = ret.replace(getServletPath(), "");
		int idx = ret.indexOf("?");
		if (idx >= 0)
			ret = ret.substring(0, idx);
		return ret;
	}

	@Override
	public String getPathTranslated() {
		throw new UtilException("Not implemented");
	}

	@Override
	public String getQueryString() {
		return uri.getQuery();
	}

	@Override
	public String getRemoteUser() {
		throw new UtilException("Not implemented");
	}

	@Override
	public String getRequestURI() {
		String ru = rawUri;
		int idx = ru.indexOf('?');
		if (idx != -1)
			ru = ru.substring(0, idx);
		return ru;
	}

	@Override
	public StringBuffer getRequestURL() {
		StringBuffer ret = new StringBuffer(getRequestURI());
		// OpenId4Java needs this ... I'm not sure if it should be here or URI ...
		ret.insert(0, "http://" + headers.get("host").get(0));
		return ret;
	}

	@Override
	public String getRequestedSessionId() {
		throw new UtilException("Not implemented");
	}

	@Override
	public String getServletPath() {
		return context.servletPath;
	}

	@Override
	public HttpSession getSession() {
		return getSession(true);
	}

	@Override
	public HttpSession getSession(boolean createIfNeeded) {
		if (session != null)
			return session;
		if (!createIfNeeded)
			return null;
		GPHttpSession ret = context.newSession();
		// Should use addCookie really, but I can't be bothered ...
		response.addHeader("Set-Cookie", "JSESSIONID="+ret.cookie+"; Path=" + context.getContextPath());
		return ret;
	}

	@Override
	public Principal getUserPrincipal() {
		throw new UtilException("Not implemented");
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		throw new UtilException("Not implemented");
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		throw new UtilException("Not implemented");
	}

	@Override
	public boolean isRequestedSessionIdFromUrl() {
		throw new UtilException("Not implemented");
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		throw new UtilException("Not implemented");
	}

	@Override
	public boolean isUserInRole(String arg0) {
		throw new UtilException("Not implemented");
	}

	public void setResponse(GPResponse response) {
		this.response = response;
	}

	public GPStaticResource getStaticResource() {
		return context.staticResource(getPathInfo()); 
	}

	@Override
	public AsyncContext getAsyncContext() {
		throw new UtilException("Not implemented");
//		if (async == null)
//			throw new UtilException("There is no async context");
//		return async;
	}

	@Override
	public DispatcherType getDispatcherType() {
		throw new UtilException("Not implemented");
	}

	@Override
	public ServletContext getServletContext() {
		throw new UtilException("Not implemented");
	}

	@Override
	public boolean isAsyncStarted() {
		throw new UtilException("Not implemented");
//		return async != null;
	}

	@Override
	public boolean isAsyncSupported() {
		throw new UtilException("Not implemented");
	}

	@Override
	public AsyncContext startAsync() {
		throw new UtilException("Not implemented");
//		return startAsync(null, null);
	}

	@Override
	public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1) {
		throw new UtilException("Not implemented");
//		logger.info(Thread.currentThread().getName()+ ": " + "Starting Async request");
//		async = new GPAsyncContext(arg0, arg1);
//		return async;
	}

	@Override
	public boolean authenticate(HttpServletResponse arg0) throws IOException,
			ServletException {
		throw new UtilException("Not implemented");
	}

	@Override
	public Part getPart(String arg0) throws IOException, IllegalStateException,
			ServletException {
		throw new UtilException("Not implemented");
	}

	@Override
	public Collection<Part> getParts() throws IOException,
			IllegalStateException, ServletException {
		throw new UtilException("Not implemented");
	}

	@Override
	public void login(String arg0, String arg1) throws ServletException {
		throw new UtilException("Not implemented");
	}

	@Override
	public void logout() throws ServletException {
		throw new UtilException("Not implemented");
	}

	public void setWebSocketHandler(InlineServerWebSocketHandler wshandler) {
		this.wshandler = wshandler;
	}
	
	@Override
	public String toString() {
		return "Request["+rawUri+"]";
	}

	public HttpServlet getServlet() {
		return context.getThisServlet();
	}
}
