package org.ziggrid.utils.http;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletInputStream;

import org.ziggrid.utils.exceptions.UtilException;

public class GPServletInputStream extends ServletInputStream {

	private final InputStream stream;
	int cnt = 0;
	private final int maxchars;
	private int pushback = -1;

	public GPServletInputStream(InputStream stream, int maxchars) {
		this.stream = stream;
		this.maxchars = maxchars;
	}

	@Override
	public int read() throws IOException {
		if (pushback != -1)
		{
			int b = pushback;
			pushback = -1;
			return b;
		}
		if (maxchars >= 0 && cnt >= maxchars)
			return -1;
		int b = stream.read();
		cnt ++; 
//		InlineServer.logger.info("read = " + (char)b + " cnt = " + cnt);
		return b;
	}

	public void flush() throws IOException {
		while (cnt < maxchars) {
			stream.read();
			cnt++;
		}
	}

	public void pushback(int b) {
		if (b == -1)
			throw new UtilException("Cannot push -1");
		pushback = b;
	}
}
