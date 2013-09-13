package org.ziggrid.utils.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;

import org.ziggrid.utils.exceptions.UtilException;

public class ProxyableConnection {
	private HttpURLConnection conn;

	ProxyableConnection(Proxy proxy, String repo) {
		try {
			if (proxy == null)
				conn = (HttpURLConnection) new URL(repo).openConnection();
			else
				conn = (HttpURLConnection) new URL(repo).openConnection(proxy);
		} catch (MalformedURLException e) {
			throw UtilException.wrap(e);
		} catch (IOException e) {
			throw UtilException.wrap(e);
		}
	}

	public InputStream getInputStream() throws IOException {
		return conn.getInputStream();
	}

}
