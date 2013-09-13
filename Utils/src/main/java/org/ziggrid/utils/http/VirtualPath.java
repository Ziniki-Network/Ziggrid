package org.ziggrid.utils.http;

import java.io.File;

public class VirtualPath {
	public final String vpath;
	public final File isTo;
	
	public VirtualPath(String vpath, File isTo) {
		this.vpath = vpath;
		this.isTo = isTo;
	}
}
