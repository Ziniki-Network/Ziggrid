package org.ziggrid.driver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemoryServerStore implements ServerStore {
	public static Logger logger = LoggerFactory.getLogger("MemoryStore");
	// Sad, but necessary, use of a static to tie together the different thread execution contexts
	private static final TreeMap<String, ServerInfo> servers = new TreeMap<String, ServerInfo>();
	
	@Override
	public void register(String type, String endpoint) {
		logger.info("Registering " + type + " " + endpoint);
		ServerInfo info = new ServerInfo(type, endpoint, new Date(), new Date());
		synchronized (servers) {
			servers.put(info.key, info);
		}
	}

	@Override
	public void delete(ServerInfo s) {
		synchronized (servers) {
			servers.remove(s.key);
		}
	}

	@Override
	public Collection<ServerInfo> list() {
		synchronized (servers) {
			return new ArrayList<ServerInfo>(servers.values());
		}
	}
	
	@Override
	public void destroy() {

	}
}
