package org.ziggrid.driver;

import java.util.Collection;

public interface ServerStore {

	Collection<ServerInfo> list();

	void register(String type, String endpoint);

	void delete(ServerInfo s);

	void destroy();
}
