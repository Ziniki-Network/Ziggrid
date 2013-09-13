package org.ziggrid.utils.http;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class GPStaticResource {

	final InputStream stream;
	final long len;

	public GPStaticResource(File f, long len, InputStream resourceAsStream) {
		this.len = len;
		this.stream = resourceAsStream;
	}

	public void close() {
		try {
			this.stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
