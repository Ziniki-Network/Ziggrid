package org.ziggrid.utils.http;

/** This is a class that can dynamically deal
 * with multiple proxy environments based on IP address
 */
public class ProxyInfo {

	public ProxyableConnection newConnection(String repo) {
		return new ProxyableConnection(null, repo);
	}

}
