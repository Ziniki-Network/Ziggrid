package org.ziggrid.utils.http;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletOutputStream;

public class GPServletOutputStream extends ServletOutputStream {
	private final OutputStream os;
	private final int myunique;
	private static int unique = 0;

	public GPServletOutputStream(OutputStream os) {
		this.os = os;
		this.myunique = ++unique;
		InlineServer.logger.trace(Thread.currentThread().getName() + ": creating " + this);
	}

	@Override
	public void write(int b) throws IOException {
//		InlineServer.logger.finest("Writing " + new String(new byte[] { (byte)b }));
		os.write(b);
	}
	
	@Override
	public void write(byte[] b) throws IOException {
//		InlineServer.logger.finest("Writing " + new String(b));
		os.write(b);
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
//		InlineServer.logger.finest("Writing " + new String(b));
		os.write(b, off, len);
		os.flush();
	}
	
	@Override
	public void close() throws IOException {
		InlineServer.logger.trace(Thread.currentThread() + " " + this + " closing stream");
		super.close();
		os.close();
	}
	
	@Override
	public String toString() {
		return "GPSos" + myunique;
	}
}
