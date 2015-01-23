package org.ziggrid.driver;

import java.util.Date;

public class ServerInfo {
	public enum ServerStatus {
		CREATED, CURRENT, EXPIRED
	}

	public final String type;
	public final String addr;
	public final String key;
	private Date whenCreated;
	private Date lastClaimedActive;
	private int expiryCount;

	public ServerInfo(String type, String addr, Date whenCreated, Date lastActive) {
		this.type = type;
		this.addr = addr;
		this.whenCreated = whenCreated;
		this.lastClaimedActive = lastActive;
		this.key = type +"/"+ addr;
	}
	
	public ServerStatus update(ServerInfo other) {
		if (other.whenCreated.after(this.whenCreated)) {
			this.whenCreated = other.whenCreated;
			this.lastClaimedActive = other.lastClaimedActive;
			expiryCount = 0;
			return ServerStatus.CREATED;
		} else if (other.lastClaimedActive.after(this.lastClaimedActive)) {
			this.lastClaimedActive = other.lastClaimedActive;
			expiryCount = 0;
			return ServerStatus.CURRENT;
		} else {
			++expiryCount;
			if (expiryCount > 4) {
				return ServerStatus.EXPIRED;
			} else
				return ServerStatus.CURRENT;
		}
	}
}
